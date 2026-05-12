package com.khalawat.android.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.khalawat.android.blocklist.BlocklistStoreImpl
import com.khalawat.android.KhalawatPreferences
import com.khalawat.android.MainActivity
import com.khalawat.android.content.ContentItem
import com.khalawat.android.content.Language
import com.khalawat.android.content.SpiritualContentProvider
import com.khalawat.android.dns.DnsProxy
import com.khalawat.android.dns.DnsProxyImpl
import com.khalawat.android.dns.DnsQuery
import com.khalawat.android.escalation.EscalationEngine
import com.khalawat.android.escalation.EscalationEngineImpl
import com.khalawat.android.persistence.AppDatabase
import com.khalawat.android.persistence.PersistentEscalationState
import com.khalawat.android.persistence.RoomSessionRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Android VPN service that intercepts DNS traffic via a TUN interface.
 *
 * Architecture:
 *   TUN -> raw IP packet -> extract UDP/DNS -> DnsResolverCoordinator ->
 *     FORWARD: relay to upstream DNS, write response back to TUN
 *     REDIRECT: return a blocked IP to TUN and emit an in-app intervention event
 *
 * The heavy logic lives in DnsResolverCoordinator (unit-testable).
 * This class is a thin Android shell: TUN setup + packet loop.
 */
class KhalawatVpnService : VpnService() {

    companion object {
        const val VPN_ADDRESS = "10.0.0.2"
        const val VPN_DNS_ROUTE = "8.8.8.8"
        const val VPN_MTU = 1500
        const val ACTION_START = "com.khalawat.android.action.START_VPN"
        const val ACTION_STOP = "com.khalawat.android.action.STOP_VPN"
        const val ACTION_OVERRIDE_INTERVENTION = "com.khalawat.android.action.OVERRIDE_INTERVENTION"
        const val ACTION_COMPLETE_STAGE_3 = "com.khalawat.android.action.COMPLETE_STAGE_3"
        const val ACTION_VPN_STARTED = "com.khalawat.android.action.VPN_STARTED"
        const val ACTION_VPN_STOPPED = "com.khalawat.android.action.VPN_STOPPED"
        const val NOTIFICATION_CHANNEL_ID = "khalawat_vpn"
        const val NOTIFICATION_ID = 1
        const val INTERVENTION_NOTIFICATION_ID = 2
        private const val TAG = "KhalawatVpn"

        @Volatile
        var isRunning: Boolean = false
        private set

        var onVpnStateChanged: ((String) -> Unit)? = null
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var coordinator: DnsResolverCoordinator? = null
    private var vpnThread: Thread? = null
    private lateinit var sessionRepo: RoomSessionRepository
    private lateinit var prefs: KhalawatPreferences
    private lateinit var escalationEngine: EscalationEngine
    private lateinit var spiritualContent: com.khalawat.android.content.SpiritualContentImpl

    @Volatile
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        prefs = KhalawatPreferences(this)
        spiritualContent = SpiritualContentProvider.get(this)
        val blocklistStore = BlocklistStoreImpl()
        val blocklistFile = kotlin.io.path.createTempFile(directory = cacheDir.toPath(), prefix = "blocklist_", suffix = ".txt").toFile()
        assets.open("blocklist.txt").bufferedReader().use { reader ->
            blocklistFile.writeText(reader.readText())
        }
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        val dnsProxy: DnsProxy = DnsProxyImpl(blocklistStore)
        escalationEngine = EscalationEngineImpl(java.time.Clock.systemUTC())
        sessionRepo = RoomSessionRepository(
            AppDatabase.getInstance(this).escalationStateDao()
        )
        restoreEscalationState(sessionRepo.loadState())
        coordinator = DnsResolverCoordinator(dnsProxy, escalationEngine, sessionRepo)
        syncDashboardState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_OVERRIDE_INTERVENTION -> {
                handleInterventionOverride(intent.getStringExtra("domain").orEmpty())
                return START_STICKY
            }
            ACTION_COMPLETE_STAGE_3 -> {
                handleStage3Complete(intent.getStringExtra("domain").orEmpty())
                return START_STICKY
            }
        }
        if (vpnInterface != null) return START_STICKY

        // Finding #10: Handle Builder.establish() returning null
        setupVpn()
        if (vpnInterface == null) {
            Log.e(TAG, "VPN establishment failed — Builder.establish() returned null")
            stopSelf()
            return START_NOT_STICKY
        }

        startPacketLoop()
        startForegroundNotification()
        isRunning = true
        broadcastVpnState(ACTION_VPN_STARTED)
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun setupVpn() {
        val builder = Builder()
            .setSession("Khalawat")
            .addAddress(VPN_ADDRESS, 32)
            .addDnsServer("8.8.8.8")
            .setMtu(VPN_MTU)
            .addDisallowedApplication(packageName)
            .addRoute(VPN_DNS_ROUTE, 32)

        vpnInterface = builder.establish()
    }

    private fun startPacketLoop() {
        running = true
        vpnThread = Thread({ packetLoop() }, "KhalawatVpnThread").apply { start() }
    }

    private fun packetLoop() {
        val vpn = vpnInterface ?: run { stopVpn(); return }
        val inputStream = FileInputStream(vpn.fileDescriptor)
        val outputStream = FileOutputStream(vpn.fileDescriptor)
        val buffer = ByteArray(VPN_MTU)

        try {
            while (running && !Thread.interrupted()) {
                val length = inputStream.read(buffer)
                if (length <= 0) continue

                val packet = buffer.copyOf(length)
                if (!isUdpDnsPacket(packet)) continue

                val dnsPayload = extractDnsPayload(packet) ?: continue
                val query = DnsQuery(dnsPayload, "8.8.8.8")
                val coord = coordinator ?: continue

                val result = coord.handleDnsPacket(query)
                when (result.action) {
                    DnsResult.Action.FORWARD -> {
                        val response = forwardDns(dnsPayload)
                        if (response != null) {
                            outputStream.write(buildResponsePacket(packet, response))
                        }
                    }
                    DnsResult.Action.REDIRECT -> {
                        emitIntervention(result.domain, result.escalationStage)
                        outputStream.write(buildDnsResponse(packet, result.redirectIp))
                    }
                }
            }
        } catch (e: Exception) {
            if (running) Log.e(TAG, "Packet loop error", e)
        } finally {
            if (running) stopVpn()
        }
    }

    // Finding #9: Wrap DatagramSocket in .use {} to prevent leak on exception.
    // Also: log DNS forwarding failures instead of silently swallowing.
    private fun forwardDns(query: ByteArray): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.send(DatagramPacket(query, query.size, InetAddress.getByName("8.8.8.8"), 53))
                val buf = ByteArray(1500)
                val resp = DatagramPacket(buf, buf.size)
                socket.soTimeout = 5000
                socket.receive(resp)
                buf.copyOf(resp.length)
            }
        } catch (e: Exception) {
            Log.w(TAG, "DNS forwarding failed", e)
            null
        }
    }

    private fun isUdpDnsPacket(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return false
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ihl + 4) return false
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    private fun extractDnsPayload(packet: ByteArray): ByteArray? {
        return try {
            val ihl = (packet[0].toInt() and 0x0F) * 4
            val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
            val dnsOffset = ihl + 8
            if (packet.size < dnsOffset) null else packet.copyOfRange(dnsOffset, ihl + udpLen)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildResponsePacket(originalPacket: ByteArray, dnsResponse: ByteArray): ByteArray {
        val ihl = (originalPacket[0].toInt() and 0x0F) * 4
        val srcIp = originalPacket.copyOfRange(12, 16)
        val dstIp = originalPacket.copyOfRange(16, 20)
        val srcPort = ((originalPacket[ihl].toInt() and 0xFF) shl 8) or (originalPacket[ihl + 1].toInt() and 0xFF)
        val dstPort = ((originalPacket[ihl + 2].toInt() and 0xFF) shl 8) or (originalPacket[ihl + 3].toInt() and 0xFF)
        val udpLen = 8 + dnsResponse.size
        val totalLen = 20 + udpLen

        val response = ByteArray(totalLen)

        // IP header
        response[0] = 0x45; response[2] = (totalLen shr 8).toByte(); response[3] = (totalLen and 0xFF).toByte()
        response[8] = 64.toByte(); response[9] = 17
        System.arraycopy(dstIp, 0, response, 12, 4)
        System.arraycopy(srcIp, 0, response, 16, 4)

        // Compute IPv4 header checksum (RFC 791)
        response[10] = 0; response[11] = 0
        var sum = 0L
        for (i in 0 until 20 step 2) {
            sum += ((response[i].toInt() and 0xFF) shl 8) or (response[i + 1].toInt() and 0xFF)
        }
        while (sum shr 16 != 0L) { sum = (sum and 0xFFFF) + (sum shr 16) }
        val checksum = (sum.toInt().inv()) and 0xFFFF
        response[10] = (checksum shr 8).toByte()
        response[11] = (checksum and 0xFF).toByte()

        // UDP header
        response[20] = (dstPort shr 8).toByte(); response[21] = (dstPort and 0xFF).toByte()
        response[22] = (srcPort shr 8).toByte(); response[23] = (srcPort and 0xFF).toByte()
        response[24] = (udpLen shr 8).toByte(); response[25] = (udpLen and 0xFF).toByte()

        // DNS payload
        System.arraycopy(dnsResponse, 0, response, 28, dnsResponse.size)

        return response
    }

    private fun buildDnsResponse(originalPacket: ByteArray, redirectIp: InetAddress?): ByteArray {
        val ihl = (originalPacket[0].toInt() and 0x0F) * 4
        val dnsQuery = originalPacket.copyOfRange(ihl + 8, originalPacket.size)
        val dnsResponse = ByteArray(dnsQuery.size + 16)
        System.arraycopy(dnsQuery, 0, dnsResponse, 0, dnsQuery.size)
        dnsResponse[2] = (dnsResponse[2].toInt() or 0x80).toByte() // QR=1
        dnsResponse[7] = 1 // ANCOUNT=1

        // Answer: pointer + A record
        val off = dnsQuery.size
        dnsResponse[off] = 0xC0.toByte(); dnsResponse[off + 1] = 0x0C
        dnsResponse[off + 3] = 1; dnsResponse[off + 5] = 1 // TYPE A, CLASS IN
        dnsResponse[off + 9] = 60 // TTL
        dnsResponse[off + 11] = 4 // RDLENGTH
        val ip = (redirectIp ?: InetAddress.getByName("0.0.0.0")).address
        System.arraycopy(ip, 0, dnsResponse, off + 12, 4)

        return buildResponsePacket(originalPacket, dnsResponse)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Khalawat VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Khalawat is actively protecting your device"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val notification = android.app.Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Khalawat Active")
            .setContentText("Protecting your device")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopVpn() {
        running = false
        isRunning = false
        vpnThread?.interrupt()
        vpnThread = null
        vpnInterface?.close()
        vpnInterface = null
        broadcastVpnState(ACTION_VPN_STOPPED)
    }

    private fun broadcastVpnState(action: String) {
        onVpnStateChanged?.invoke(action)
        val intent = Intent(action)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun restoreEscalationState(state: PersistentEscalationState?) {
        if (state == null) return
        escalationEngine.restore(
            stage = state.stage,
            lastRequestTimeMillis = state.lastRequestTime,
            lastOverrideTimeMillis = state.lastOverrideTime,
            cooldownEndTimeMillis = state.cooldownEndTime
        )
    }

    private fun syncDashboardState() {
        val startOfDay = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        KhalawatRuntimeState.updateDashboard(
            DashboardSnapshot(
                currentStage = escalationEngine.getCurrentStage(),
                interventionCountToday = sessionRepo.getInterventionCountSince(startOfDay)
            )
        )
    }

    private fun emitIntervention(domain: String, stage: com.khalawat.android.escalation.EscalationStage) {
        val state = buildOverlayState(domain = domain, stage = stage)
        prefs.pendingInterventionDomain = domain
        prefs.pendingInterventionStage = stage.name
        prefs.pendingInterventionStartedAt = state.startedAtMillis
        KhalawatRuntimeState.showIntervention(state)
        syncDashboardState()
        showInterventionNotification(state)
    }

    private fun buildOverlayState(
        domain: String,
        stage: com.khalawat.android.escalation.EscalationStage
    ): InterventionOverlayState {
        val language = parseLanguage(prefs.selectedLanguage)
        val startedAt = System.currentTimeMillis()
        return when (stage) {
            com.khalawat.android.escalation.EscalationStage.STAGE_1 -> {
                val item = nextReflection(language)
                InterventionOverlayState(
                    stage = stage,
                    domain = domain,
                    startedAtMillis = startedAt,
                    title = "Pause before you proceed",
                    body = "A blocked request for $domain was intercepted. Take 15 seconds and sit with this reminder before making the next choice.",
                    content = item,
                )
            }
            com.khalawat.android.escalation.EscalationStage.STAGE_2 -> {
                InterventionOverlayState(
                    stage = stage,
                    domain = domain,
                    startedAtMillis = startedAt,
                    title = "Slow down and reset",
                    body = "The urge is still active. Breathe, repeat the dhikr, and give yourself 30 seconds before deciding anything else.",
                    dhikrItems = List(3) { spiritualContent.nextDhikr(language) },
                )
            }
            com.khalawat.android.escalation.EscalationStage.STAGE_3,
            com.khalawat.android.escalation.EscalationStage.COOLING -> {
                val item = nextReflection(language)
                InterventionOverlayState(
                    stage = com.khalawat.android.escalation.EscalationStage.STAGE_3,
                    domain = domain,
                    startedAtMillis = startedAt,
                    title = "Hard stop",
                    body = "Khalawat has moved into a two-minute hard stop. Stay here, breathe, and let the moment pass.",
                    content = item,
                )
            }
        }
    }

    private fun nextReflection(language: Language): ContentItem {
        val ayah = spiritualContent.nextAyah(language)
        return if (ayah.arabic.isNotBlank()) ayah else spiritualContent.nextHadith(language)
    }

    private fun parseLanguage(value: String): Language {
        return try {
            Language.valueOf(value)
        } catch (_: IllegalArgumentException) {
            Language.EN
        }
    }

    private fun showInterventionNotification(state: InterventionOverlayState) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            INTERVENTION_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = android.app.Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(state.title)
            .setContentText("Blocked: ${state.domain}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(INTERVENTION_NOTIFICATION_ID, notification)
    }

    private fun handleInterventionOverride(domain: String) {
        if (domain.isBlank()) return
        val result = coordinator?.override(domain) ?: return
        emitIntervention(result.domain, result.escalationStage)
    }

    private fun handleStage3Complete(domain: String) {
        if (domain.isBlank()) return
        coordinator?.onStage3Complete(domain)
        prefs.pendingInterventionDomain = null
        prefs.pendingInterventionStage = null
        prefs.pendingInterventionStartedAt = 0L
        KhalawatRuntimeState.clearIntervention()
        syncDashboardState()
        getSystemService(NotificationManager::class.java)
            .cancel(INTERVENTION_NOTIFICATION_ID)
    }
}

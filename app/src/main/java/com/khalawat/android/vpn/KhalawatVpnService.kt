package com.khalawat.android.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.khalawat.android.blocklist.BlocklistStoreImpl
import com.khalawat.android.dns.DnsProxy
import com.khalawat.android.dns.DnsProxyImpl
import com.khalawat.android.dns.DnsQuery
import com.khalawat.android.escalation.EscalationEngine
import com.khalawat.android.escalation.EscalationEngineImpl
import com.khalawat.android.persistence.AppDatabase
import com.khalawat.android.persistence.RoomSessionRepository
import com.khalawat.android.server.InterventionServer
import java.io.File
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
 *     REDIRECT: return 127.0.0.1 (InterventionServer) to TUN
 *
 * The heavy logic lives in DnsResolverCoordinator (unit-testable).
 * This class is a thin Android shell: TUN setup + packet loop.
 */
class KhalawatVpnService : VpnService() {

    companion object {
        const val VPN_ADDRESS = "10.0.0.2"
        const val VPN_ROUTE = "0.0.0.0"
        const val VPN_MTU = 1500
        const val ACTION_START = "com.khalawat.android.action.START_VPN"
        const val ACTION_STOP = "com.khalawat.android.action.STOP_VPN"
        const val NOTIFICATION_CHANNEL_ID = "khalawat_vpn"
        const val NOTIFICATION_ID = 1
        private const val TAG = "KhalawatVpn"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var coordinator: DnsResolverCoordinator? = null
    private var interventionServer: InterventionServer? = null
    private var vpnThread: Thread? = null

    @Volatile
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val blocklistStore = BlocklistStoreImpl()
        assets.open("blocklist.txt").bufferedReader().use { reader ->
            val tempFile = File(cacheDir, "blocklist.txt")
            tempFile.writeText(reader.readText())
            blocklistStore.loadBlocklist(tempFile.absolutePath)
        }

        val dnsProxy: DnsProxy = DnsProxyImpl(blocklistStore)
        val escalationEngine: EscalationEngine = EscalationEngineImpl(java.time.Clock.systemUTC())
        val sessionRepo = RoomSessionRepository(
            AppDatabase.getInstance(this).escalationStateDao()
        )
        coordinator = DnsResolverCoordinator(dnsProxy, escalationEngine, sessionRepo)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        if (vpnInterface != null) return START_STICKY

        // Finding #10: Handle Builder.establish() returning null
        setupVpn()
        if (vpnInterface == null) {
            Log.e(TAG, "VPN establishment failed — Builder.establish() returned null")
            stopSelf()
            return START_NOT_STICKY
        }

        startInterventionServer()
        startPacketLoop()
        startForegroundNotification()
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
        vpnInterface = Builder()
            .setSession("Khalawat")
            .addAddress(VPN_ADDRESS, 32)
            .addRoute(VPN_ROUTE, 0)
            .addDnsServer("8.8.8.8")
            .setMtu(VPN_MTU)
            .addDisallowedApplication(packageName)
            .establish()
    }

    private fun startInterventionServer() {
        val server = InterventionServer(
            spiritualContent = com.khalawat.android.content.SpiritualContentImpl(),
            assetLoader = { path -> assets.open(path) }
        )
        server.start()
        interventionServer = server
    }

    private fun startPacketLoop() {
        running = true
        vpnThread = Thread({ packetLoop() }, "KhalawatVpnThread").apply { start() }
    }

    private fun packetLoop() {
        val vpn = vpnInterface ?: return
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
                        outputStream.write(buildDnsResponse(packet, result.redirectIp))
                    }
                }
            }
        } catch (e: Exception) {
            if (running) Log.e(TAG, "Packet loop error", e)
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
        val ip = (redirectIp ?: InetAddress.getByName("127.0.0.1")).address
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
        vpnThread?.interrupt()
        vpnThread = null
        interventionServer?.stop()
        interventionServer = null
        vpnInterface?.close()
        vpnInterface = null
    }
}

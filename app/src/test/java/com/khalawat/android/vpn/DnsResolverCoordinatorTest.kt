package com.khalawat.android.vpn

import com.google.common.truth.Truth.assertThat
import com.khalawat.android.dns.DnsProxy
import com.khalawat.android.dns.DnsQuery
import com.khalawat.android.dns.DnsResponse
import com.khalawat.android.escalation.EscalationEngine
import com.khalawat.android.escalation.EscalationStage
import com.khalawat.android.escalation.EscalationState
import com.khalawat.android.persistence.PersistentEscalationState
import com.khalawat.android.persistence.SessionRepository
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.time.Clock
import java.time.Instant

/**
 * Issue #8: VpnService — TDD for the packet routing coordinator.
 *
 * DnsResolverCoordinator is the logic core: it receives raw DNS packets,
 * routes them through DnsProxy + EscalationEngine, and decides:
 *   - BLOCKED → escalate + redirect to intervention server
 *   - ALLOWED → forward to upstream DNS
 *
 * The actual VpnService is a thin Android shell around this coordinator.
 */
class DnsResolverCoordinatorTest {

    private lateinit var coordinator: DnsResolverCoordinator
    private lateinit var fakeDnsProxy: FakeDnsProxy
    private lateinit var fakeEscalationEngine: FakeEscalationEngine
    private lateinit var fakeSessionRepo: FakeSessionRepoForVpn

    @Before
    fun setUp() {
        fakeDnsProxy = FakeDnsProxy()
        fakeEscalationEngine = FakeEscalationEngine()
        fakeSessionRepo = FakeSessionRepoForVpn()
        coordinator = DnsResolverCoordinator(fakeDnsProxy, fakeEscalationEngine, fakeSessionRepo)
    }

    // --- Allowed domain flows through ---

    @Test
    fun `allowed domain returns forwarded response`() {
        fakeDnsProxy.nextResponse = DnsResponse.Forwarded(ByteArray(10) { 0 })
        val result = coordinator.handleDnsPacket(buildQuery("clean.com"))
        assertThat(result.action).isEqualTo(DnsResult.Action.FORWARD)
    }

    @Test
    fun `allowed domain does not trigger escalation`() {
        fakeDnsProxy.nextResponse = DnsResponse.Forwarded(ByteArray(10) { 0 })
        coordinator.handleDnsPacket(buildQuery("clean.com"))
        assertThat(fakeEscalationEngine.blockedDomains).isEmpty()
    }

    // --- Blocked domain triggers escalation ---

    @Test
    fun `blocked domain returns redirect to intervention server`() {
        fakeDnsProxy.nextResponse = DnsResponse.Blocked(InetAddress.getByName("127.0.0.1"))
        val result = coordinator.handleDnsPacket(buildQuery("bad.com"))
        assertThat(result.action).isEqualTo(DnsResult.Action.REDIRECT)
    }

    @Test
    fun `blocked domain triggers escalation engine`() {
        fakeDnsProxy.nextResponse = DnsResponse.Blocked(InetAddress.getByName("127.0.0.1"))
        coordinator.handleDnsPacket(buildQuery("bad.com"))
        assertThat(fakeEscalationEngine.blockedDomains).contains("bad.com")
    }

    @Test
    fun `blocked domain escalation returns stage info`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_1
        fakeDnsProxy.nextResponse = DnsResponse.Blocked(InetAddress.getByName("127.0.0.1"))
        val result = coordinator.handleDnsPacket(buildQuery("bad.com"))
        assertThat(result.escalationStage).isEqualTo(EscalationStage.STAGE_1)
    }

    @Test
    fun `stage 2 escalation on second blocked request`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_2
        fakeDnsProxy.nextResponse = DnsResponse.Blocked(InetAddress.getByName("127.0.0.1"))
        val result = coordinator.handleDnsPacket(buildQuery("bad.com"))
        assertThat(result.escalationStage).isEqualTo(EscalationStage.STAGE_2)
    }

    @Test
    fun `stage 3 escalation on third blocked request`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_3
        fakeDnsProxy.nextResponse = DnsResponse.Blocked(InetAddress.getByName("127.0.0.1"))
        val result = coordinator.handleDnsPacket(buildQuery("bad.com"))
        assertThat(result.escalationStage).isEqualTo(EscalationStage.STAGE_3)
    }

    // --- Override flow ---

    @Test
    fun `override advances escalation stage`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_2
        val result = coordinator.override("bad.com")
        assertThat(fakeEscalationEngine.overriddenDomains).contains("bad.com")
        assertThat(result.escalationStage).isEqualTo(EscalationStage.STAGE_2)
    }

    @Test
    fun `override logs to session repository`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_2
        coordinator.override("bad.com")
        assertThat(fakeSessionRepo.overrideLogs).hasSize(1)
        assertThat(fakeSessionRepo.overrideLogs[0].first).isEqualTo("bad.com")
    }

    // --- Session persistence on state change ---

    @Test
    fun `blocked request persists escalation state`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_1
        fakeDnsProxy.nextResponse = DnsResponse.Blocked(InetAddress.getByName("127.0.0.1"))
        coordinator.handleDnsPacket(buildQuery("bad.com"))
        assertThat(fakeSessionRepo.savedState).isNotNull()
        assertThat(fakeSessionRepo.savedState!!.stage).isEqualTo(EscalationStage.STAGE_1)
    }

    @Test
    fun `override persists escalation state`() {
        fakeEscalationEngine.nextStage = EscalationStage.STAGE_2
        coordinator.override("bad.com")
        assertThat(fakeSessionRepo.savedState).isNotNull()
        assertThat(fakeSessionRepo.savedState!!.stage).isEqualTo(EscalationStage.STAGE_2)
    }

    // --- Stage 3 complete triggers cooling ---

    @Test
    fun `stage3 complete transitions to cooling`() {
        fakeEscalationEngine.nextStageOnComplete = EscalationStage.COOLING
        val result = coordinator.onStage3Complete("bad.com")
        assertThat(result.escalationStage).isEqualTo(EscalationStage.COOLING)
    }

    @Test
    fun `stage3 complete persists cooling state`() {
        fakeEscalationEngine.nextStageOnComplete = EscalationStage.COOLING
        coordinator.onStage3Complete("bad.com")
        assertThat(fakeSessionRepo.savedState!!.stage).isEqualTo(EscalationStage.COOLING)
    }

    // --- Cooldown expiry resets ---

    @Test
    fun `reset clears escalation and session state`() {
        coordinator.reset()
        assertThat(fakeEscalationEngine.wasReset).isTrue()
        assertThat(fakeSessionRepo.cleared).isTrue()
    }

    // --- Helpers ---

    private fun buildQuery(domain: String): DnsQuery {
        // Build a minimal DNS query packet for the given domain
        val labels = domain.split(".")
        var size = 12 // header
        for (label in labels) { size += 1 + label.length }
        size += 1 // null terminator
        size += 4 // qtype + qclass

        val packet = ByteArray(size)
        var offset = 12
        for (label in labels) {
            packet[offset++] = label.length.toByte()
            for (ch in label.toByteArray(Charsets.US_ASCII)) {
                packet[offset++] = ch
            }
        }
        packet[offset++] = 0 // null terminator
        // QType A (1) + QClass IN (1)
        packet[offset++] = 0; packet[offset++] = 1
        packet[offset++] = 0; packet[offset++] = 1

        return DnsQuery(data = packet, upstreamDns = "8.8.8.8")
    }
}

// --- Fakes ---

class FakeDnsProxy : DnsProxy {
    var nextResponse: DnsResponse = DnsResponse.Forwarded(ByteArray(0))
    override fun resolve(query: DnsQuery): DnsResponse = nextResponse
}

class FakeEscalationEngine : EscalationEngine {
    var nextStage: EscalationStage = EscalationStage.STAGE_1
    var nextStageOnComplete: EscalationStage = EscalationStage.COOLING
    val blockedDomains = mutableListOf<String>()
    val overriddenDomains = mutableListOf<String>()
    var wasReset = false

    private var currentTime = Instant.now()
    private var currentCooldown: Instant? = null

    override fun onBlockedRequest(domain: String): EscalationState {
        blockedDomains.add(domain)
        return EscalationState(
            stage = nextStage,
            currentDomain = domain,
            cooldownEndTime = currentCooldown,
            lastRequestTime = currentTime,
            lastOverrideTime = null
        )
    }

    override fun override(domain: String): EscalationState {
        overriddenDomains.add(domain)
        return EscalationState(
            stage = nextStage,
            currentDomain = domain,
            cooldownEndTime = currentCooldown,
            lastRequestTime = currentTime,
            lastOverrideTime = currentTime
        )
    }

    override fun onStage3Complete(domain: String): EscalationState {
        currentCooldown = currentTime.plusSeconds(600)
        return EscalationState(
            stage = nextStageOnComplete,
            currentDomain = domain,
            cooldownEndTime = currentCooldown,
            lastRequestTime = currentTime,
            lastOverrideTime = null
        )
    }

    override fun getCurrentStage(): EscalationStage = nextStage
    override fun getCooldownEndTime(): Instant? = currentCooldown
    override fun getLastRequestTime(): Instant = currentTime
    override fun reset() { wasReset = true; nextStage = EscalationStage.STAGE_1 }
}

class FakeSessionRepoForVpn : SessionRepository {
    var savedState: PersistentEscalationState? = null
    var cleared = false
    val overrideLogs = mutableListOf<Triple<String, EscalationStage, Long>>()

    override fun loadState(): PersistentEscalationState? = savedState
    override fun saveState(state: PersistentEscalationState) { savedState = state }
    override fun clearState() { cleared = true; savedState = null }
    override fun logOverride(domain: String, stage: EscalationStage, timestamp: Long) {
        overrideLogs.add(Triple(domain, stage, timestamp))
    }
    override fun getOverrideCountSince(sinceTimestamp: Long): Int =
        overrideLogs.count { it.third > sinceTimestamp }
    override fun clearOverrideLogs() { overrideLogs.clear() }
}

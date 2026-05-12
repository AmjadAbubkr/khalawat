package com.khalawat.android.vpn

import com.khalawat.android.dns.DnsProxy
import com.khalawat.android.dns.DnsQuery
import com.khalawat.android.dns.DnsResponse
import com.khalawat.android.escalation.EscalationEngine
import com.khalawat.android.escalation.EscalationStage
import com.khalawat.android.escalation.EscalationState
import com.khalawat.android.persistence.PersistentEscalationState
import com.khalawat.android.persistence.SessionRepository
import java.net.InetAddress

/**
 * Result of handling a DNS packet through the coordinator.
 */
data class DnsResult(
    val action: Action,
    val escalationStage: EscalationStage = EscalationStage.STAGE_1,
    val domain: String = "",
    val redirectIp: InetAddress? = null
) {
    enum class Action { FORWARD, REDIRECT }
}

/**
 * The logic core that sits between the VPN TUN interface and the network.
 *
 * For each DNS packet:
 *   1. Ask DnsProxy if domain is blocked
 *   2. If blocked → escalate + return REDIRECT (to intervention server)
 *   3. If allowed → return FORWARD (to upstream DNS)
 *
 * Also persists escalation state via SessionRepository.
 */
class DnsResolverCoordinator(
    private val dnsProxy: DnsProxy,
    private val escalationEngine: EscalationEngine,
    private val sessionRepository: SessionRepository
) {
    fun handleDnsPacket(query: DnsQuery): DnsResult {
        val response = dnsProxy.resolve(query)

        return when (response) {
            is DnsResponse.Blocked -> {
                val domain = com.khalawat.android.dns.DnsPacketParser.extractDomain(query.data) ?: "unknown"
                val state = escalationEngine.onBlockedRequest(domain)
                persistState(state)
                sessionRepository.logIntervention(domain, state.stage, System.currentTimeMillis())
                DnsResult(
                    action = DnsResult.Action.REDIRECT,
                    escalationStage = state.stage,
                    domain = domain,
                    redirectIp = response.redirectIp
                )
            }
            is DnsResponse.Forwarded -> {
                DnsResult(action = DnsResult.Action.FORWARD)
            }
        }
    }

    fun override(domain: String): DnsResult {
        val state = escalationEngine.override(domain)
        persistState(state)
        sessionRepository.logOverride(domain, state.stage, System.currentTimeMillis())
        return DnsResult(
            action = DnsResult.Action.REDIRECT,
            escalationStage = state.stage,
            domain = domain
        )
    }

    fun onStage3Complete(domain: String): DnsResult {
        val state = escalationEngine.onStage3Complete(domain)
        persistState(state)
        return DnsResult(
            action = DnsResult.Action.REDIRECT,
            escalationStage = state.stage,
            domain = domain
        )
    }

    fun reset() {
        escalationEngine.reset()
        sessionRepository.clearState()
    }

    private fun persistState(state: EscalationState) {
        sessionRepository.saveState(
            PersistentEscalationState(
                stage = state.stage,
                lastRequestTime = state.lastRequestTime.toEpochMilli(),
                lastOverrideTime = state.lastOverrideTime?.toEpochMilli(),
                cooldownEndTime = state.cooldownEndTime?.toEpochMilli()
            )
        )
    }
}

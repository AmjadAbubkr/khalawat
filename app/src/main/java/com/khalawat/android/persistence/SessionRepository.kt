package com.khalawat.android.persistence

import com.khalawat.android.escalation.EscalationStage

/**
 * Persisted snapshot of the escalation state machine.
 * Stored in Room so it survives process death.
 */
data class PersistentEscalationState(
    val stage: EscalationStage,
    val lastRequestTime: Long,
    val lastOverrideTime: Long?,
    val cooldownEndTime: Long?
)

/**
 * Abstraction over escalation state persistence.
 * Room-backed in production; fake in unit tests.
 */
interface SessionRepository {
    fun loadState(): PersistentEscalationState?
    fun saveState(state: PersistentEscalationState)
    fun clearState()
    fun logIntervention(domain: String, stage: EscalationStage, timestamp: Long)
    fun logOverride(domain: String, stage: EscalationStage, timestamp: Long)
    fun getInterventionCountSince(sinceTimestamp: Long): Int
    fun getOverrideCountSince(sinceTimestamp: Long): Int
    fun clearOverrideLogs()
}

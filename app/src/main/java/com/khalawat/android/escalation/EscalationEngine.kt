package com.khalawat.android.escalation

import java.time.Instant

enum class EscalationStage {
    STAGE_1,   // Soft intercept: 15s countdown + spiritual reminder
    STAGE_2,   // Active deflection: 30s breathing + dhikr
    STAGE_3,   // Hard lock: 2 min, no override
    COOLING    // Post-Stage3: all blocked domains → Stage 3 directly (10 min)
}

data class EscalationState(
    val stage: EscalationStage,
    val currentDomain: String,
    val cooldownEndTime: Instant? = null,
    val lastRequestTime: Instant,
    val lastOverrideTime: Instant? = null
)

interface EscalationEngine {
    fun onBlockedRequest(domain: String): EscalationState
    fun override(domain: String): EscalationState
    fun onStage3Complete(domain: String): EscalationState
    fun getCurrentStage(): EscalationStage
    fun getCooldownEndTime(): Instant?
    fun getLastRequestTime(): Instant
    fun reset()
}

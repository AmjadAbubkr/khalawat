package com.khalawat.android.escalation

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class EscalationEngineImpl(
    private val clock: Clock
) : EscalationEngine {

    companion object {
        val COOLING_DURATION: Duration = Duration.ofMinutes(10)
        val IDLE_RESET_THRESHOLD: Duration = Duration.ofMinutes(30)
        val POST_OVERRIDE_NO_RESET_WINDOW: Duration = Duration.ofMinutes(10)
    }

    private var currentStage: EscalationStage = EscalationStage.STAGE_1
    private var lastRequestTime: Instant = clock.instant()
    private var lastOverrideTime: Instant? = null
    private var cooldownEndTime: Instant? = null
    private var lastDomain: String = ""

    override fun onBlockedRequest(domain: String): EscalationState {
        val now = clock.instant()

        // Check if cooling period has expired
        if (currentStage == EscalationStage.COOLING) {
            val cooldown = cooldownEndTime
            if (cooldown != null && now.isAfter(cooldown)) {
                // Cooling expired — full reset
                currentStage = EscalationStage.STAGE_1
                cooldownEndTime = null
                lastOverrideTime = null
            }
        }

        // Check sliding window idle reset (only if not in post-override window)
        if (currentStage != EscalationStage.COOLING && currentStage != EscalationStage.STAGE_1) {
            val overrideTime = lastOverrideTime
            val timeSinceLastRequest = Duration.between(lastRequestTime, now)

            if (timeSinceLastRequest.compareTo(IDLE_RESET_THRESHOLD) > 0) {
                // 30 minutes idle — reset one level
                if (overrideTime == null || Duration.between(overrideTime, now).compareTo(POST_OVERRIDE_NO_RESET_WINDOW) > 0) {
                    currentStage = when (currentStage) {
                        EscalationStage.STAGE_3 -> EscalationStage.STAGE_2
                        EscalationStage.STAGE_2 -> EscalationStage.STAGE_1
                        else -> currentStage
                    }
                }
            }
        }

        // During cooling, all blocked domains go straight to Stage 3
        val effectiveStage = if (currentStage == EscalationStage.COOLING) {
            EscalationStage.STAGE_3
        } else {
            currentStage
        }

        lastDomain = domain
        lastRequestTime = now

        return EscalationState(
            stage = effectiveStage,
            currentDomain = domain,
            cooldownEndTime = cooldownEndTime,
            lastRequestTime = now,
            lastOverrideTime = lastOverrideTime
        )
    }

    override fun override(domain: String): EscalationState {
        val now = clock.instant()
        lastOverrideTime = now

        currentStage = when (currentStage) {
            EscalationStage.STAGE_1 -> EscalationStage.STAGE_2
            EscalationStage.STAGE_2 -> EscalationStage.STAGE_3
            EscalationStage.STAGE_3 -> EscalationStage.STAGE_3 // Can't go higher
            EscalationStage.COOLING -> EscalationStage.COOLING  // Can't override during cooling
        }

        return EscalationState(
            stage = currentStage,
            currentDomain = domain,
            cooldownEndTime = cooldownEndTime,
            lastRequestTime = lastRequestTime,
            lastOverrideTime = now
        )
    }

    override fun onStage3Complete(domain: String): EscalationState {
        val now = clock.instant()
        cooldownEndTime = now.plus(COOLING_DURATION)
        currentStage = EscalationStage.COOLING

        return EscalationState(
            stage = EscalationStage.COOLING,
            currentDomain = domain,
            cooldownEndTime = cooldownEndTime,
            lastRequestTime = lastRequestTime,
            lastOverrideTime = lastOverrideTime
        )
    }

    override fun restore(
        stage: EscalationStage,
        lastRequestTimeMillis: Long,
        lastOverrideTimeMillis: Long?,
        cooldownEndTimeMillis: Long?
    ) {
        currentStage = stage
        lastRequestTime = Instant.ofEpochMilli(lastRequestTimeMillis).atZone(ZoneOffset.UTC).toInstant()
        lastOverrideTime = lastOverrideTimeMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toInstant() }
        cooldownEndTime = cooldownEndTimeMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toInstant() }
    }

    override fun getCurrentStage(): EscalationStage = currentStage

    override fun getCooldownEndTime(): Instant? = cooldownEndTime

    override fun getLastRequestTime(): Instant = lastRequestTime

    override fun reset() {
        currentStage = EscalationStage.STAGE_1
        lastRequestTime = clock.instant()
        lastOverrideTime = null
        cooldownEndTime = null
        lastDomain = ""
    }
}

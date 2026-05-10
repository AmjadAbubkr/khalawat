package com.khalawat.android.persistence

import com.khalawat.android.escalation.EscalationStage

/**
 * Room-backed implementation of SessionRepository.
 *
 * Converts between the app's PersistentEscalationState and Room entities.
 */
class RoomSessionRepository(private val dao: EscalationStateDao) : SessionRepository {

    override fun loadState(): PersistentEscalationState? {
        val entity = dao.getState() ?: return null
        return PersistentEscalationState(
            stage = entity.stage,
            lastRequestTime = entity.lastRequestTime,
            lastOverrideTime = entity.lastOverrideTime,
            cooldownEndTime = entity.cooldownEndTime
        )
    }

    override fun saveState(state: PersistentEscalationState) {
        dao.saveState(
            stage = state.stage,
            lastRequestTime = state.lastRequestTime,
            lastOverrideTime = state.lastOverrideTime,
            cooldownEndTime = state.cooldownEndTime
        )
    }

    override fun clearState() {
        dao.clearState()
    }

    override fun logOverride(domain: String, stage: EscalationStage, timestamp: Long) {
        dao.logOverride(
            OverrideLogEntity(
                domain = domain,
                timestamp = timestamp,
                stageReached = stage
            )
        )
    }

    override fun getOverrideCountSince(sinceTimestamp: Long): Int {
        return dao.getOverrideCountSince(sinceTimestamp)
    }

    override fun clearOverrideLogs() {
        dao.clearOverrideLogs()
    }
}

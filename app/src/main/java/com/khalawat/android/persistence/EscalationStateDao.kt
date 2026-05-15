package com.khalawat.android.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.khalawat.android.escalation.EscalationStage

@Dao
interface EscalationStateDao {

    @Query("SELECT * FROM escalation_state WHERE id = 1")
    fun getState(): EscalationStateEntity?

    @Query("INSERT OR REPLACE INTO escalation_state (id, stage, lastRequestTime, lastOverrideTime, cooldownEndTime) VALUES (1, :stage, :lastRequestTime, :lastOverrideTime, :cooldownEndTime)")
    fun saveState(stage: EscalationStage, lastRequestTime: Long, lastOverrideTime: Long?, cooldownEndTime: Long?)

    @Query("DELETE FROM escalation_state")
    fun clearState()

    @Insert
    fun logOverride(entity: OverrideLogEntity)

    @Insert
    fun logIntervention(entity: InterventionLogEntity)

    @Query("SELECT * FROM override_log ORDER BY timestamp DESC")
    fun getOverrideLogs(): List<OverrideLogEntity>

    @Query("SELECT COUNT(*) FROM intervention_log WHERE timestamp > :sinceTimestamp")
    fun getInterventionCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM override_log WHERE timestamp > :sinceTimestamp")
    fun getOverrideCountSince(sinceTimestamp: Long): Int

    @Query("DELETE FROM intervention_log")
    fun clearInterventionLogs()

    @Query("DELETE FROM override_log")
    fun clearOverrideLogs()
}

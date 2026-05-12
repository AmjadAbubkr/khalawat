package com.khalawat.android.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.khalawat.android.escalation.EscalationStage

@Entity(tableName = "escalation_state")
data class EscalationStateEntity(
    @PrimaryKey val id: Int = 1,
    val stage: EscalationStage,
    val lastRequestTime: Long,
    val lastOverrideTime: Long? = null,
    val cooldownEndTime: Long? = null
)

@Entity(tableName = "override_log")
data class OverrideLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val timestamp: Long,
    val stageReached: EscalationStage
)

@Entity(tableName = "intervention_log")
data class InterventionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val timestamp: Long,
    val stageReached: EscalationStage
)

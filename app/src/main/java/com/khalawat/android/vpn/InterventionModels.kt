package com.khalawat.android.vpn

import com.khalawat.android.content.ContentItem
import com.khalawat.android.escalation.EscalationStage

data class InterventionOverlayState(
    val stage: EscalationStage,
    val domain: String,
    val startedAtMillis: Long,
    val title: String,
    val body: String,
    val content: ContentItem? = null,
    val dhikrItems: List<ContentItem> = emptyList(),
)

data class DashboardSnapshot(
    val currentStage: EscalationStage = EscalationStage.STAGE_1,
    val interventionCountToday: Int = 0,
)

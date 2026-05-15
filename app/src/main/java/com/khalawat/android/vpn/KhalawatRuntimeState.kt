package com.khalawat.android.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object KhalawatRuntimeState {
    private val _dashboard = MutableStateFlow(DashboardSnapshot())
    val dashboard = _dashboard.asStateFlow()

    private val _intervention = MutableStateFlow<InterventionOverlayState?>(null)
    val intervention = _intervention.asStateFlow()

    fun updateDashboard(snapshot: DashboardSnapshot) {
        _dashboard.value = snapshot
    }

    fun showIntervention(state: InterventionOverlayState) {
        _intervention.value = state
    }

    fun clearIntervention() {
        _intervention.value = null
    }
}

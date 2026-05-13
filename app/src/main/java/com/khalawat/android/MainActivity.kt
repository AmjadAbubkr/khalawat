package com.khalawat.android

import android.content.Intent
import android.app.NotificationManager
import android.os.Build
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.khalawat.android.content.Language
import com.khalawat.android.antitamper.AntiTamperState
import com.khalawat.android.antitamper.DisableScreen
import com.khalawat.android.escalation.EscalationStage
import com.khalawat.android.onboarding.OnboardingFlow
import com.khalawat.android.onboarding.OnboardingState
import com.khalawat.android.ui.DashboardScreen
import com.khalawat.android.ui.InterventionOverlay
import com.khalawat.android.ui.theme.KhalawatTheme
import com.khalawat.android.vpn.DashboardSnapshot
import com.khalawat.android.vpn.InterventionOverlayState
import com.khalawat.android.vpn.KhalawatRuntimeState
import com.khalawat.android.vpn.KhalawatVpnService
import com.khalawat.android.persistence.AppDatabase
import com.khalawat.android.persistence.RoomSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var prefs: KhalawatPreferences
    private lateinit var sessionRepo: RoomSessionRepository
    private val onboardingState = OnboardingState()
    private val antiTamperState = AntiTamperState()
    private var isVpnActiveState by mutableStateOf(false)

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            onVpnPermissionGranted()
        }
    }

    private val vpnStateCallback: (String) -> Unit = { action ->
        when (action) {
            KhalawatVpnService.ACTION_VPN_STARTED -> {
                isVpnActiveState = true
                prefs.isVpnActive = true
                prefs.userStoppedVpn = false
            }
            KhalawatVpnService.ACTION_VPN_STOPPED -> {
                isVpnActiveState = false
                prefs.isVpnActive = false
                if (!prefs.userStoppedVpn && prefs.isOnboardingComplete) {
                    val intent = VpnService.prepare(this@MainActivity)
                    if (intent == null) startVpn()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb(),
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb(),
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        prefs = KhalawatPreferences(this)
        sessionRepo = RoomSessionRepository(AppDatabase.getInstance(this).escalationStateDao())
        isVpnActiveState = prefs.isVpnActive
        onboardingState.changeLanguage(parseLanguage(prefs.selectedLanguage))

        prefs.companionPin?.let { antiTamperState.setCompanionPinRequired(it) }
        antiTamperState.restoreDisconnectCount(prefs.disconnectCount)
        restorePendingIntervention()
        refreshDashboardSnapshot()

        KhalawatVpnService.onVpnStateChanged = vpnStateCallback

        if (prefs.isOnboardingComplete && !isVpnActiveState && !prefs.userStoppedVpn) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startVpn()
            }
        }

        setContent {
            KhalawatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KhalawatApp(
                        isOnboardingComplete = prefs.isOnboardingComplete,
                        isVpnActive = isVpnActiveState,
                        onboardingState = onboardingState,
                        antiTamperState = antiTamperState,
                        onRequestVpnPermission = { requestVpnPermission() },
                        onStartVpn = { startVpn() },
                        onStopVpn = { stopVpn() },
                        onDismissIntervention = { dismissIntervention() },
                        onAdvanceIntervention = { domain -> advanceIntervention(domain) },
                        onCompleteStage3 = { domain -> completeStage3(domain) },
                        onClearOnboarding = { prefs.clear() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.isOnboardingComplete) {
            val vpnActive = KhalawatVpnService.isRunning
            if (isVpnActiveState != vpnActive) {
                isVpnActiveState = vpnActive
                prefs.isVpnActive = vpnActive
            }
            refreshDashboardSnapshot()
            restorePendingIntervention()
            if (!vpnActive && !prefs.userStoppedVpn) {
                val intent = VpnService.prepare(this)
                if (intent == null) {
                    startVpn()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.disconnectCount = antiTamperState.disconnectCount
        KhalawatVpnService.onVpnStateChanged = null
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            onVpnPermissionGranted()
        }
    }

    private fun onVpnPermissionGranted() {
        onboardingState.grantVpnPermission()
        prefs.isOnboardingComplete = true
        prefs.companionPin = onboardingState.companionPin
        prefs.parentMessage = onboardingState.parentMessage
        prefs.selectedLanguage = onboardingState.selectedLanguage.name
        if (onboardingState.companionPin != null) {
            antiTamperState.setCompanionPinRequired(onboardingState.companionPin!!)
        }
        startVpn()
    }

    private fun startVpn() {
        prefs.userStoppedVpn = false
        val intent = Intent(this, KhalawatVpnService::class.java).apply {
            action = KhalawatVpnService.ACTION_START
        }
        startService(intent)
    }

    private fun stopVpn() {
        prefs.userStoppedVpn = true
        antiTamperState.recordDisconnect()
        prefs.disconnectCount = antiTamperState.disconnectCount
        val intent = Intent(this, KhalawatVpnService::class.java).apply {
            action = KhalawatVpnService.ACTION_STOP
        }
        startService(intent)
        isVpnActiveState = false
        prefs.isVpnActive = false
        antiTamperState.releaseHold()
    }

    private fun dismissIntervention() {
        prefs.pendingInterventionDomain = null
        prefs.pendingInterventionStage = null
        prefs.pendingInterventionStartedAt = 0L
        getSystemService(NotificationManager::class.java)
            .cancel(KhalawatVpnService.INTERVENTION_NOTIFICATION_ID)
        KhalawatRuntimeState.clearIntervention()
    }

    private fun advanceIntervention(domain: String) {
        val intent = Intent(this, KhalawatVpnService::class.java).apply {
            action = KhalawatVpnService.ACTION_OVERRIDE_INTERVENTION
            putExtra("domain", domain)
        }
        startService(intent)
    }

    private fun completeStage3(domain: String) {
        val intent = Intent(this, KhalawatVpnService::class.java).apply {
            action = KhalawatVpnService.ACTION_COMPLETE_STAGE_3
            putExtra("domain", domain)
        }
        startService(intent)
    }

    private fun refreshDashboardSnapshot() {
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                val startOfDay = java.time.LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val persisted = sessionRepo.loadState()
                DashboardSnapshot(
                    currentStage = persisted?.stage ?: EscalationStage.STAGE_1,
                    interventionCountToday = sessionRepo.getInterventionCountSince(startOfDay)
                )
            }
            KhalawatRuntimeState.updateDashboard(snapshot)
        }
    }

    private fun restorePendingIntervention() {
        val stageName = prefs.pendingInterventionStage ?: return
        val domain = prefs.pendingInterventionDomain ?: return
        val stage = try {
            EscalationStage.valueOf(stageName)
        } catch (_: IllegalArgumentException) {
            return
        }
        if (KhalawatRuntimeState.intervention.value == null) {
            KhalawatRuntimeState.showIntervention(
                InterventionOverlayState(
                    stage = stage,
                    domain = domain,
                    startedAtMillis = prefs.pendingInterventionStartedAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    title = when (stage) {
                        EscalationStage.STAGE_1 -> "Pause before you proceed"
                        EscalationStage.STAGE_2 -> "Slow down and reset"
                        EscalationStage.STAGE_3, EscalationStage.COOLING -> "Hard stop"
                    },
                    body = "Khalawat intercepted a blocked request for $domain. Open the app and complete the intervention flow.",
                )
            )
        }
    }

    private fun parseLanguage(value: String): Language {
        return try {
            Language.valueOf(value)
        } catch (_: IllegalArgumentException) {
            Language.EN
        }
    }
}

sealed class AppScreen {
    object Onboarding : AppScreen()
    object Dashboard : AppScreen()
    object Disable : AppScreen()
}

@Composable
fun KhalawatApp(
    isOnboardingComplete: Boolean,
    isVpnActive: Boolean,
    onboardingState: OnboardingState,
    antiTamperState: AntiTamperState,
    onRequestVpnPermission: () -> Unit,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onDismissIntervention: () -> Unit,
    onAdvanceIntervention: (String) -> Unit,
    onCompleteStage3: (String) -> Unit,
    onClearOnboarding: () -> Unit = {}
) {
    val dashboard by KhalawatRuntimeState.dashboard.collectAsState()
    val overlayState by KhalawatRuntimeState.intervention.collectAsState()
    val showOnboarding by remember {
        derivedStateOf { !isOnboardingComplete && !onboardingState.isComplete }
    }

    var showDisableScreen by remember { mutableStateOf(false) }

    val currentScreen = when {
        showOnboarding -> AppScreen.Onboarding
        showDisableScreen -> AppScreen.Disable
        else -> AppScreen.Dashboard
    }

    val screenOrder = remember { listOf(AppScreen.Onboarding, AppScreen.Dashboard, AppScreen.Disable) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val fromIdx = screenOrder.indexOf(initialState)
            val toIdx = screenOrder.indexOf(targetState)
            val direction = if (toIdx > fromIdx) 1 else -1
            (
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) { fullWidth -> (fullWidth * 0.08f * direction).toInt() } +
                    fadeIn(animationSpec = tween(durationMillis = 220))
                ) togetherWith (
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) { fullWidth -> (-fullWidth * 0.05f * direction).toInt() } +
                    fadeOut(animationSpec = tween(durationMillis = 180))
                )
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.Onboarding -> {
                OnboardingFlow(
                    state = onboardingState,
                    onRequestVpnPermission = onRequestVpnPermission,
                    onFinish = { }
                )
            }
            AppScreen.Disable -> {
                DisableScreen(
                    state = antiTamperState,
                    onDisable = {
                        onStopVpn()
                        showDisableScreen = false
                    },
                    onCancel = {
                        showDisableScreen = false
                    }
                )
            }
            AppScreen.Dashboard -> {
                DashboardScreen(
                    isVpnActive = isVpnActive,
                    currentStage = dashboard.currentStage,
                    overrideCountToday = dashboard.interventionCountToday,
                    onToggleVpn = {
                        if (isVpnActive) {
                            showDisableScreen = true
                        } else {
                            onStartVpn()
                        }
                    },
                    onShowDisable = { showDisableScreen = true }
                )
            }
        }
    }

    overlayState?.let { intervention ->
        InterventionOverlay(
            state = intervention,
            onDismiss = onDismissIntervention,
            onAdvance = onAdvanceIntervention,
            onStage3Complete = onCompleteStage3
        )
    }
}

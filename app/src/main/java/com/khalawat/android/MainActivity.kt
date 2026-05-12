package com.khalawat.android

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.khalawat.android.antitamper.AntiTamperState
import com.khalawat.android.antitamper.DisableScreen
import com.khalawat.android.escalation.EscalationStage
import com.khalawat.android.onboarding.OnboardingFlow
import com.khalawat.android.onboarding.OnboardingState
import com.khalawat.android.ui.DashboardScreen
import com.khalawat.android.ui.theme.KhalawatTheme
import com.khalawat.android.vpn.KhalawatVpnService

class MainActivity : ComponentActivity() {

    private lateinit var prefs: KhalawatPreferences
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
                    startVpn()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = KhalawatPreferences(this)
        isVpnActiveState = prefs.isVpnActive

        prefs.companionPin?.let { antiTamperState.setCompanionPinRequired(it) }
        antiTamperState.restoreDisconnectCount(prefs.disconnectCount)

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
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
        val safeLanguage = try {
            com.khalawat.android.content.Language.valueOf(prefs.selectedLanguage)
        } catch (_: IllegalArgumentException) {
            com.khalawat.android.content.Language.EN
        }
        onboardingState.changeLanguage(safeLanguage)
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
    onClearOnboarding: () -> Unit = {}
) {
    val showOnboarding by remember {
        derivedStateOf { !isOnboardingComplete && !onboardingState.isComplete }
    }

    var showDisableScreen by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf(EscalationStage.STAGE_1) }
    var overrideCount by remember { mutableStateOf(0) }

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
            slideInHorizontally(animationSpec = tween(350)) { fullWidth -> fullWidth * direction } togetherWith
            slideOutHorizontally(animationSpec = tween(350)) { fullWidth -> -fullWidth * direction }
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
                    currentStage = currentStage,
                    overrideCountToday = overrideCount,
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
}

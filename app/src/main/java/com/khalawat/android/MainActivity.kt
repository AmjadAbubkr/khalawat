package com.khalawat.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                KhalawatVpnService.ACTION_VPN_STARTED -> {
                    isVpnActiveState = true
                    prefs.isVpnActive = true
                }
                KhalawatVpnService.ACTION_VPN_STOPPED -> {
                    isVpnActiveState = false
                    prefs.isVpnActive = false
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

        val filter = IntentFilter().apply {
            addAction(KhalawatVpnService.ACTION_VPN_STARTED)
            addAction(KhalawatVpnService.ACTION_VPN_STOPPED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(vpnStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(vpnStateReceiver, filter)
        }

        if (prefs.isOnboardingComplete && !isVpnActiveState) {
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

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(vpnStateReceiver) } catch (_: Exception) {}
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
        val intent = Intent(this, KhalawatVpnService::class.java).apply {
            action = KhalawatVpnService.ACTION_START
        }
        startService(intent)
    }

    private fun stopVpn() {
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
            slideInHorizontally(animationSpec = tween(400)) { fullWidth -> fullWidth * direction / 3 } +
                fadeIn(animationSpec = tween(300)) togetherWith
            slideOutHorizontally(animationSpec = tween(400)) { fullWidth -> -fullWidth * direction / 3 } +
                fadeOut(animationSpec = tween(300))
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
                    onToggleVpn = onStartVpn,
                    onShowDisable = { showDisableScreen = true }
                )
            }
        }
    }
}

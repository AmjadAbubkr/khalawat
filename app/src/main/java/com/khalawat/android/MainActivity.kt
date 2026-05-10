package com.khalawat.android

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
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

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            onVpnPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = KhalawatPreferences(this)

        // Restore companion PIN into anti-tamper state
        prefs.companionPin?.let { antiTamperState.setCompanionPinRequired(it) }

        setContent {
            KhalawatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KhalawatApp(
                        isOnboardingComplete = prefs.isOnboardingComplete,
                        isVpnActive = prefs.isVpnActive,
                        onboardingState = onboardingState,
                        antiTamperState = antiTamperState,
                        onRequestVpnPermission = { requestVpnPermission() },
                        onStartVpn = { startVpn() },
                        onStopVpn = { stopVpn() }
                    )
                }
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // Permission already granted
            onVpnPermissionGranted()
        }
    }

    private fun onVpnPermissionGranted() {
        onboardingState.grantVpnPermission()
        onboardingState.setSelectedLanguage(
            com.khalawat.android.content.Language.valueOf(prefs.selectedLanguage)
        )
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
        prefs.isVpnActive = true
    }

    private fun stopVpn() {
        val intent = Intent(this, KhalawatVpnService::class.java).apply {
            action = KhalawatVpnService.ACTION_STOP
        }
        startService(intent)
        prefs.isVpnActive = false
        antiTamperState.releaseHold()
    }
}

@Composable
fun KhalawatApp(
    isOnboardingComplete: Boolean,
    isVpnActive: Boolean,
    onboardingState: OnboardingState,
    antiTamperState: AntiTamperState,
    onRequestVpnPermission: () -> Unit,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    var showOnboarding by remember { mutableStateOf(!isOnboardingComplete) }
    var showDisableScreen by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf(EscalationStage.STAGE_1) }
    var overrideCount by remember { mutableStateOf(0) }

    when {
        showOnboarding -> {
            OnboardingFlow(
                state = onboardingState,
                onRequestVpnPermission = {
                    onRequestVpnPermission()
                    if (onboardingState.isComplete) {
                        showOnboarding = false
                    }
                },
                onFinish = { showOnboarding = false }
            )
        }
        showDisableScreen -> {
            DisableScreen(
                state = antiTamperState,
                onDisable = {
                    onStopVpn()
                    showDisableScreen = false
                },
                onCancel = { showDisableScreen = false }
            )
        }
        else -> {
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

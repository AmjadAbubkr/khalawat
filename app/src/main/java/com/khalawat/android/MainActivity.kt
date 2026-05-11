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
                        onStopVpn = { stopVpn() },
                        onClearOnboarding = { prefs.clear() }
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

/**
 * Root composable for the Khalawat app.
 *
 * Key design: `showOnboarding` is derived from BOTH the persisted
 * `isOnboardingComplete` flag (from SharedPreferences) AND the live
 * `onboardingState.isComplete` (mutableStateOf). This ensures:
 * - Fresh installs always start onboarding
 * - Returning users (who completed onboarding previously) skip to dashboard
 * - Completing onboarding in the CURRENT session reactively dismisses it
 *   without any imperative "showOnboarding = false" call
 */
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
    // showOnboarding is true only while onboarding has never been completed
    // (neither in this session nor in a previous one).
    // When onboardingState.isComplete becomes true (via grantVpnPermission),
    // this reactive derivation automatically recomposes and hides onboarding.
    val showOnboarding by remember {
        derivedStateOf { !isOnboardingComplete && !onboardingState.isComplete }
    }

    var showDisableScreen by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf(EscalationStage.STAGE_1) }
    var overrideCount by remember { mutableStateOf(0) }

    when {
        showOnboarding -> {
            OnboardingFlow(
                state = onboardingState,
                onRequestVpnPermission = onRequestVpnPermission,
                onFinish = {
                    // onFinish is called when user clicks "Start Protecting"
                    // after VPN permission is granted. The reactive
                    // derivedStateOf already hides onboarding, but we keep
                    // this callback for any future side-effects.
                }
            )
        }
        showDisableScreen -> {
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

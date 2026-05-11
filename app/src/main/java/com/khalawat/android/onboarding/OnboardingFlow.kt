package com.khalawat.android.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khalawat.android.content.Language

/**
 * Full onboarding flow — 5 screens driven by OnboardingState.
 */
@Composable
fun OnboardingFlow(
    state: OnboardingState,
    onRequestVpnPermission: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state.currentScreen) {
                OnboardingScreen.WELCOME -> WelcomeScreen()
                OnboardingScreen.PURPOSE -> PurposeScreen()
                OnboardingScreen.HOW_IT_WORKS -> HowItWorksScreen()
                OnboardingScreen.COMPANION_PIN -> CompanionPinScreen(state)
                OnboardingScreen.VPN_PERMISSION -> VpnPermissionScreen(
                    state = state,
                    onRequestPermission = onRequestVpnPermission,
                    onFinish = onFinish
                )
            }
        }

        // Navigation buttons
        if (state.currentScreen != OnboardingScreen.VPN_PERMISSION) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.currentScreen != OnboardingScreen.WELCOME) {
                    OutlinedButton(onClick = { state.back() }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }
                Button(onClick = { state.next() }) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen() {
    Text(
        text = "\u062E\u064E\u0644\u064E\u0627\u0648\u064E\u0629",
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Khalawat",
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Your voluntary companion for digital self-discipline.\nPrivacy-first. No accounts. No tracking.\nJust you and your commitment to Allah.",
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
    )
}

@Composable
private fun PurposeScreen() {
    Text("Why Khalawat?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "Khalawat helps you break the urge window when haram content appears.\n\n" +
        "It doesn't just block \u2014 it intervenes with spiritual reminders,\n" +
        "breathing exercises, and escalating barriers that give you\n" +
        "1\u20133 minutes to choose the right path.\n\n" +
        "\u0643\u064F\u0644\u0651\u064F \u0627\u0628\u0652\u0646\u0650 \u0622\u062F\u064E\u0645\u064E \u062E\u064E\u0637\u0651\u064E\u0627\u0621\u064C\n" +
        "\"Every son of Adam sins\" \u2014 Tirmidhi 2499",
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
}

@Composable
private fun HowItWorksScreen() {
    Text("How It Works", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    val steps = listOf(
        "1. Khalawat runs a local VPN that checks DNS requests" to "No data leaves your device",
        "2. Blocked sites get a spiritual intervention page" to "Not a blank block \u2014 a reminder",
        "3. Stage 1: 15s pause + Quran ayah" to "A gentle nudge",
        "4. Stage 2: 30s breathing + dhikr counter" to "Active deflection",
        "5. Stage 3: 2-min hard lock" to "Full barrier",
        "6. 10-min cooling after Stage 3" to "Reset window"
    )
    steps.forEach { (title, subtitle) ->
        Text(title, fontWeight = FontWeight.Medium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CompanionPinScreen(state: OnboardingState) {
    // Finding #6: rememberSaveable preserves values across config changes
    var pinInput by rememberSaveable { mutableStateOf("") }
    var parentMsg by rememberSaveable { mutableStateOf("") }

    Text("Companion PIN", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "Optionally set a PIN that a parent or accountability partner controls.\n" +
        "They'll need it to disable Khalawat.",
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(24.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("Enable Companion PIN")
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = state.companionPinEnabled,
            onCheckedChange = { state.enableCompanionPin(it) }
        )
    }

    if (state.companionPinEnabled) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = pinInput,
            onValueChange = { newPin ->
                if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
                    pinInput = newPin
                    if (newPin.length == 4) state.setCompanionPin(newPin)
                }
            },
            label = { Text("4-digit PIN") },
            modifier = Modifier.fillMaxWidth(0.6f),
            singleLine = true,
            // Finding #5: Mask PIN input and show numeric password keyboard
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = parentMsg,
            onValueChange = {
                parentMsg = it
                state.updateParentMessage(it)
            },
            label = { Text("Custom message from parent") },
            modifier = Modifier.fillMaxWidth(0.8f),
            minLines = 2,
            maxLines = 3
        )
    }
}

@Composable
private fun VpnPermissionScreen(
    state: OnboardingState,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit
) {
    Text("Almost There!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "Khalawat needs VPN permission to intercept DNS requests.\n\n" +
        "This creates a local VPN on your device.\n" +
        "No data is sent anywhere \u2014 all processing is local.",
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(32.dp))

    if (!state.vpnPermissionGranted) {
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Grant VPN Permission")
        }
    } else {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Permission granted",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Start Protecting")
        }
    }
}

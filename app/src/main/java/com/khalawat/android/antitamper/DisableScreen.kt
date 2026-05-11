package com.khalawat.android.antitamper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Anti-tamper disable screen with 30-sec hold button.
 * Shown when user tries to disable Khalawat VPN.
 *
 * AntiTamperState uses mutableStateOf — Compose observes isHoldActive,
 * holdProgress, and isHoldComplete directly. The LaunchedEffect key
 * is state.isHoldActive, which now properly triggers recomposition
 * when startHold() or releaseHold() is called.
 */
@Composable
fun DisableScreen(
    state: AntiTamperState,
    onDisable: () -> Unit,
    onCancel: () -> Unit
) {
    // Track elapsed time locally to feed updateHoldProgress()
    var holdElapsed by remember { mutableStateOf(0L) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Timer that advances hold progress while the hold is active.
    // Key = state.isHoldActive (now a mutableStateOf — changes trigger relaunch)
    LaunchedEffect(state.isHoldActive) {
        if (state.isHoldActive) {
            val startTime = System.currentTimeMillis() - holdElapsed
            while (state.isHoldActive && !state.isHoldComplete) {
                delay(100)
                holdElapsed = System.currentTimeMillis() - startTime
                state.updateHoldProgress(holdElapsed)
            }
        }
    }

    // Reset local elapsed when hold is released
    if (!state.isHoldActive && holdElapsed != 0L) {
        holdElapsed = 0L
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Are you sure?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show spiritual reminder during hold
        if (state.isHoldActive && state.spiritualReminder != null) {
            val reminder = state.spiritualReminder!!
            Card(
                modifier = Modifier.fillMaxWidth(0.85f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(reminder.arabic, fontSize = 22.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(reminder.translation, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text(
                        reminder.source,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hold-to-disable button
        if (!state.isHoldComplete) {
            val buttonText = if (state.isHoldActive) {
                "Keep holding... ${(state.holdDurationSeconds - holdElapsed / 1000).coerceAtLeast(0)}s"
            } else {
                "I want to disable Khalawat"
            }

            Button(
                onClick = {
                    if (!state.isHoldActive) {
                        holdElapsed = 0L
                        state.startHold()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(buttonText)
            }

            if (state.isHoldActive) {
                LinearProgressIndicator(
                    progress = { state.holdProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(top = 8.dp)
                )
            }
        } else {
            // Hold complete - companion PIN gate if enabled
            if (state.requiresCompanionPin) {
                Text("Enter Companion PIN to disable:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        pinError = false
                    },
                    label = { Text("4-digit PIN") },
                    isError = pinError,
                    singleLine = true,
                    modifier = Modifier.width(160.dp)
                )
                if (pinError) {
                    Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (state.verifyCompanionPin(pinInput)) {
                            onDisable()
                        } else {
                            pinError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirm Disable")
                }
            } else {
                Button(
                    onClick = onDisable,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirm Disable")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = {
            holdElapsed = 0L
            state.releaseHold()
            onCancel()
        }) {
            Text("Go Back \u2014 I want to stay protected")
        }
    }
}

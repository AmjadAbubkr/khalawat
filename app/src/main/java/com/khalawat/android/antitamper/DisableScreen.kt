package com.khalawat.android.antitamper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.autoMirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DisableScreen(
    state: AntiTamperState,
    onDisable: () -> Unit,
    onCancel: () -> Unit,
) {
    var holdElapsed by remember { mutableIntStateOf(0) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(state.isHoldActive) {
        if (state.isHoldActive) {
            val start = System.currentTimeMillis()
            while (state.isHoldActive && !state.isHoldComplete) {
                delay(100)
                holdElapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
                state.updateHoldProgress(holdElapsed * 1000L)
            }
        }
    }

    if (!state.isHoldActive && holdElapsed != 0) holdElapsed = 0

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Are you sure?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                ),
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Spiritual Reminder
            if (state.spiritualReminder != null) {
                val r = state.spiritualReminder!!
                Card(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(r.arabic, fontSize = 22.sp, textAlign = TextAlign.Center, lineHeight = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(r.translation, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text(r.source, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Hold-to-disable
            if (!state.isHoldComplete) {
                val btnText = if (state.isHoldActive) {
                    "Keep holding… ${(30 - holdElapsed).coerceAtLeast(0)}s"
                } else {
                    "I want to disable Khalawat"
                }

                Button(
                    onClick = {
                        if (!state.isHoldActive) {
                            holdElapsed = 0
                            state.startHold()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(btnText, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }

                if (state.isHoldActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.holdProgress },
                        modifier = Modifier.fillMaxWidth(0.8f).height(6.dp),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            } else {
                // After hold complete, PIN gate
                if (state.requiresCompanionPin) {
                    Text("Enter Companion PIN to disable:", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput, onValueChange = { pinInput = it; pinError = false },
                        label = { Text("4-digit PIN") }, isError = pinError,
                        singleLine = true, modifier = Modifier.width(160.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        )
                    )
                    if (pinError) {
                        Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (state.verifyCompanionPin(pinInput)) onDisable() else pinError = true
                        },
                        modifier = Modifier.fillMaxWidth(0.6f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(28.dp),
                    ) { Text("Confirm Disable") }
                } else {
                    Button(
                        onClick = onDisable,
                        modifier = Modifier.fillMaxWidth(0.6f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(28.dp),
                    ) { Text("Confirm Disable") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { holdElapsed = 0; state.releaseHold(); onCancel() },
                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Go Back — I want to stay protected")
            }
        }
    }
}

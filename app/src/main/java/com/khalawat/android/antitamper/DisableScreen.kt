package com.khalawat.android.antitamper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Are you sure?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (state.spiritualReminder != null) {
                val r = state.spiritualReminder!!
                val infiniteTransition = rememberInfiniteTransition(label = "ayah_glow")
                val ayahGlowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ayah_glow_alpha"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = ayahGlowAlpha)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = r.arabic,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = r.translation,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = r.source,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            AnimatedContent(
                targetState = state.isHoldComplete,
                transitionSpec = {
                    fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 } togetherWith
                    fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 }
                },
                label = "disable_step"
            ) { holdComplete ->
                if (!holdComplete) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val btnText = if (state.isHoldActive) {
                            "Keep holding… ${(30 - holdElapsed).coerceAtLeast(0)}s"
                        } else {
                            "I want to disable Khalawat"
                        }

                        val buttonScale by animateFloatAsState(
                            targetValue = if (state.isHoldActive) 1f else 0.98f,
                            animationSpec = tween(200),
                            label = "hold_btn_scale"
                        )

                        Button(
                            onClick = {
                                if (!state.isHoldActive) {
                                    holdElapsed = 0
                                    state.startHold()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp).scale(buttonScale),
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
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.requiresCompanionPin) {
                            Text(
                                "Enter Companion PIN to disable:",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { new: String -> pinInput = new; pinError = false },
                                label = { Text("4-digit PIN") },
                                isError = pinError,
                                singleLine = true,
                                modifier = Modifier.width(160.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                ),
                            )
                            if (pinError) {
                                Text(
                                    "Incorrect PIN",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (state.verifyCompanionPin(pinInput)) {
                                        state.recordDisconnect()
                                        onDisable()
                                    } else {
                                        pinError = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.6f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(28.dp),
                            ) { Text("Confirm Disable") }
                        } else {
                            Button(
                                onClick = {
                                    state.recordDisconnect()
                                    onDisable()
                                },
                                modifier = Modifier.fillMaxWidth(0.6f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(28.dp),
                            ) { Text("Confirm Disable") }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    holdElapsed = 0
                    state.releaseHold()
                    onCancel()
                },
                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Go Back — I want to stay protected")
            }
        }
    }
}

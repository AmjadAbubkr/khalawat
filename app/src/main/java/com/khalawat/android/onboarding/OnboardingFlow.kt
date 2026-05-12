package com.khalawat.android.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OnboardingFlow(
    state: OnboardingState,
    onRequestVpnPermission: () -> Unit,
    onFinish: () -> Unit,
) {
    val totalSteps = OnboardingScreen.entries.size

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (state.currentScreen != OnboardingScreen.VPN_PERMISSION) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1 until totalSteps).forEach { step ->
                        val isActive = step < (state.currentScreen.ordinal + 1)
                        val isCurrent = step == (state.currentScreen.ordinal + 1)
                        val color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                        val animatedWidth by animateFloatAsState(
                            targetValue = if (isCurrent) 40f else 8f,
                            animationSpec = tween(300),
                            label = "progress_width"
                        )
                        Box(
                            modifier = Modifier
                                .width(animatedWidth.dp)
                                .height(4.dp)
                                .background(color, RoundedCornerShape(4.dp))
                                .padding(horizontal = 2.dp),
                        ) {}
                        if (step < totalSteps - 1) Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
            AnimatedContent(
                targetState = state.currentScreen,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally(animationSpec = tween(350)) { fullWidth -> fullWidth * dir } togetherWith
                    slideOutHorizontally(animationSpec = tween(350)) { fullWidth -> -fullWidth * dir }
                },
                label = "onboarding_page"
                ) { screen ->
                    when (screen) {
                        OnboardingScreen.WELCOME -> WelcomeScreen()
                        OnboardingScreen.PURPOSE -> PurposeScreen()
                        OnboardingScreen.HOW_IT_WORKS -> HowItWorksScreen()
                        OnboardingScreen.COMPANION_PIN -> CompanionPinScreen(state)
                        OnboardingScreen.VPN_PERMISSION -> VpnPermissionScreen(
                            state = state,
                            onRequestPermission = onRequestVpnPermission,
                            onFinish = onFinish,
                        )
                    }
                }
            }

            if (state.currentScreen != OnboardingScreen.VPN_PERMISSION) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.currentScreen != OnboardingScreen.WELCOME) {
                        TextButton(onClick = { state.back() }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }
                    Button(onClick = { state.next() }) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/* ────────────────────── Welcome Screen ────────────────────────────── */
@Composable
private fun WelcomeScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(500, delayMillis = 0),
                initialOffsetY = { -it / 4 }
            )
        ) {
            Box(
                modifier = Modifier.size(120.dp).background(
                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                    CircleShape
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(500, delayMillis = 100),
                initialOffsetY = { it / 6 }
            )
        ) {
            Text(
                text = "خُلُوَّات",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(500, delayMillis = 200),
                initialOffsetY = { it / 6 }
            )
        ) {
            Text(
                text = "Khalawat",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(500, delayMillis = 300),
                initialOffsetY = { it / 6 }
            )
        ) {
            Text(
                text = "Your voluntary companion for digital self-discipline.\nPrivacy-first. No accounts. No tracking.\nJust you and your commitment to Allah.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/* ───────────────────── Purpose Screen ─────────────────────────────── */
@Composable
private fun PurposeScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(400, delayMillis = 0),
                initialOffsetY = { -it / 4 }
            )
        ) {
            Text(
                text = "Why Khalawat?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(500, delayMillis = 100),
                initialOffsetY = { it / 6 }
            )
        ) {
            Text(
                text = "Khalawat helps you break the urge window when haram content appears.\n\nIt doesn't just block — it intervenes with spiritual reminders, breathing exercises, and escalating barriers that give you 1–3 minutes to choose the right path.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                animationSpec = tween(500, delayMillis = 200),
                initialOffsetY = { it / 6 }
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "كُلُّ ابْنِ آدَمَ خَطَّاءٌ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 36.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Every son of Adam sins — Tirmidhi 2499",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

/* ─────────────────── How It Works Screen ─────────────────────────── */
@Composable
private fun HowItWorksScreen() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "How It Works",
            style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))

        val steps = listOf(
            Step("Local VPN", "Checks DNS requests locally", Icons.Default.CloudOff),
            Step("Blocking", "Blocked sites show an intervention page", Icons.Default.AutoAwesome),
            Step("Stage 1", "15-second pause with a Quran Ayah", Icons.Default.Pause),
            Step("Stage 2", "30-second breathing + Dhikr", Icons.Default.Air),
            Step("Stage 3", "2-minute hard lock", Icons.Default.Lock),
            Step("Cooling", "10-minute cooldown resets the cycle", Icons.Default.HourglassEmpty),
        )

        steps.forEachIndexed { index, step ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    animationSpec = tween(400, delayMillis = index * 80),
                    initialOffsetY = { it / 6 }
                )
            ) {
                StepRow(step = step, stepNumber = index + 1)
            }
            if (index < steps.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private data class Step(val title: String, val desc: String, val icon: ImageVector)

@Composable
private fun StepRow(step: Step, stepNumber: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(18.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$stepNumber", style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.desc,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

/* ─────────────────── Companion PIN Screen ─────────────────────────── */
@Composable
private fun CompanionPinScreen(state: OnboardingState) {
    val focusManager = LocalFocusManager.current
    var pinInput by rememberSaveable { mutableStateOf(state.companionPin ?: "") }
    var parentMsg by rememberSaveable { mutableStateOf(state.parentMessage) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Companion PIN",
            style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Optionally set a PIN that a parent or accountability partner controls. They'll need it to disable Khalawat.",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Enable Companion PIN",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Adds a layer of external accountability.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.companionPinEnabled,
                    onCheckedChange = { enabled ->
                        state.enableCompanionPin(enabled)
                        if (!enabled) {
                            pinInput = ""
                            parentMsg = ""
                        }
                    },
                )
            }
        }

        if (state.companionPinEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Set 4-Digit PIN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { new: String ->
                            if (new.length <= 4 && new.all { it.isDigit() }) {
                                pinInput = new
                                if (new.length == 4) state.setCompanionPin(new)
                            }
                        },
                        label = { Text("4-digit PIN") },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = parentMsg,
                        onValueChange = { new: String ->
                            parentMsg = new
                            state.updateParentMessage(new)
                        },
                        label = { Text("Custom message from parent") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    )
                }
            }
        }
    }
}

/* ─────────────────── VPN Permission Screen ─────────────────────────── */
@Composable
private fun VpnPermissionScreen(
    state: OnboardingState,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit,
) {
    var isActivating by remember { mutableStateOf(false) }
    var isActivated by remember { mutableStateOf(false) }

    LaunchedEffect(state.vpnPermissionGranted) {
        if (state.vpnPermissionGranted && !isActivated) {
            isActivating = true
            delay(2000)
            isActivating = false
            isActivated = true
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        when {
            isActivating -> {
                val infiniteTransition = rememberInfiniteTransition(label = "activating_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )
                Box(
                    modifier = Modifier.size(100.dp).scale(pulseScale).background(
                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Activating Protection…",
                    style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Setting up your local VPN shield",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            isActivated -> {
                val activatedGlow = rememberInfiniteTransition(label = "activated_glow")
                val checkScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(600),
                    label = "check_scale"
                )
                val glowAlpha by activatedGlow.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow"
                )
                Box(
                    modifier = Modifier.size(100.dp).background(
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Protected",
                        modifier = Modifier.size(56.dp).scale(checkScale),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "You're Protected!",
                    style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Khalawat is now actively guarding your device.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Protecting")
                }
            }
            else -> {
                Box(
                    modifier = Modifier.size(80.dp).background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(40.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Shield, contentDescription = null,
                        modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Almost There!",
                    style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Khalawat needs VPN permission to intercept DNS requests.\n\nThis creates a local VPN on your device. No data is sent anywhere — all processing is local.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Spacer(modifier = Modifier.height(32.dp))

                if (!state.vpnPermissionGranted) {
                    Button(
                        onClick = {
                            onRequestPermission()
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant VPN Permission")
                    }
                }
            }
        }
    }
}

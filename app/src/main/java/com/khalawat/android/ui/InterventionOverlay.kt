package com.khalawat.android.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.khalawat.android.escalation.EscalationStage
import com.khalawat.android.vpn.InterventionOverlayState
import kotlinx.coroutines.delay

@Composable
fun InterventionOverlay(
    state: InterventionOverlayState,
    onDismiss: () -> Unit,
    onAdvance: (String) -> Unit,
    onStage3Complete: (String) -> Unit,
) {
    val minDialogHeight = (LocalConfiguration.current.screenHeightDp / 3).dp
    val totalSeconds = when (state.stage) {
        EscalationStage.STAGE_1 -> 15
        EscalationStage.STAGE_2 -> 30
        EscalationStage.STAGE_3,
        EscalationStage.COOLING -> 120
    }
    var secondsRemaining by remember(state.domain, state.stage, state.startedAtMillis) {
        val elapsed = ((System.currentTimeMillis() - state.startedAtMillis) / 1000L).toInt()
        mutableIntStateOf((totalSeconds - elapsed).coerceAtLeast(0))
    }

    LaunchedEffect(state.domain, state.stage, state.startedAtMillis) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
        if (state.stage == EscalationStage.STAGE_3 || state.stage == EscalationStage.COOLING) {
            onStage3Complete(state.domain)
        }
    }

    Dialog(
        onDismissRequest = {},
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(min = minDialogHeight),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = state.domain,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = state.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(18.dp)
                        ) {
                            when (state.stage) {
                                EscalationStage.STAGE_1 -> Stage1Body(state)
                                EscalationStage.STAGE_2 -> Stage2Body(state)
                                EscalationStage.STAGE_3,
                                EscalationStage.COOLING -> Stage3Body(state)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text(
                            text = if (secondsRemaining > 0) {
                                "${secondsRemaining}s remaining"
                            } else {
                                "You can choose your next step now"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (state.stage != EscalationStage.STAGE_3 && state.stage != EscalationStage.COOLING) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Step Back")
                                }
                                Button(
                                    onClick = { onAdvance(state.domain) },
                                    enabled = secondsRemaining == 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (state.stage == EscalationStage.STAGE_1) {
                                            "Proceed to Stage 2"
                                        } else {
                                            "Proceed to Stage 3"
                                        }
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onStage3Complete(state.domain) },
                                    enabled = secondsRemaining == 0,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Return to Dashboard")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stage1Body(state: InterventionOverlayState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.content?.arabic.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.content?.translation.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = state.content?.source.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun Stage2Body(state: InterventionOverlayState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.dhikrItems.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = item.arabic,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.translation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Stage3Body(state: InterventionOverlayState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.content?.arabic.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.content?.translation.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

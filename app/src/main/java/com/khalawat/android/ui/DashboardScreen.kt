package com.khalawat.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khalawat.android.escalation.EscalationStage

@Composable
fun DashboardScreen(
    isVpnActive: Boolean,
    currentStage: EscalationStage,
    overrideCountToday: Int,
    onToggleVpn: () -> Unit,
    onShowDisable: () -> Unit,
) {
    val statusColor = if (isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val statusText = if (isVpnActive) "Protected" else "Unprotected"
    val statusIcon = if (isVpnActive) Icons.Default.Shield else Icons.Default.Warning

    val animatedScale by animateFloatAsState(
        targetValue = if (isVpnActive) 1f else 0.97f,
        animationSpec = tween(220),
        label = "shield_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Assalamu Alaikum",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = "Peace and clarity upon your path.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.matchParentSize()
                        .background(statusColor.copy(alpha = 0.06f), CircleShape)
                )
                Surface(
                    shape = CircleShape,
                    color = if (isVpnActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    tonalElevation = 4.dp,
                    modifier = Modifier.size(150.dp).scale(animatedScale),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = statusIcon, contentDescription = statusText,
                            modifier = Modifier.size(48.dp), tint = statusColor,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            color = statusColor, textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Today's Discipline", style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DashboardStatItem("Interventions", overrideCountToday.toString())
                        DashboardStatItem("Current Level", stageLabel(currentStage))
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = { if (isVpnActive) onShowDisable() else onToggleVpn() },
                modifier = Modifier.fillMaxWidth(0.75f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVpnActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(28.dp),
            ) {
                Icon(
                    if (isVpnActive) Icons.Default.PowerSettingsNew else Icons.Default.Lock,
                    contentDescription = null, modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isVpnActive) "Disable Khalawat" else "Enable Shield")
            }
        }
    }
}

@Composable
private fun DashboardStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

private fun stageLabel(stage: EscalationStage): String = when (stage) {
    EscalationStage.STAGE_1 -> "1 — Gentle"
    EscalationStage.STAGE_2 -> "2 — Active"
    EscalationStage.STAGE_3 -> "3 — Hard Lock"
    EscalationStage.COOLING -> "Cooling"
}

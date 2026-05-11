package com.khalawat.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khalawat.android.antitamper.AntiTamperState
import com.khalawat.android.antitamper.DisableScreen
import com.khalawat.android.escalation.EscalationStage

/**
 * Main dashboard shown when VPN is active.
 */
@Composable
fun DashboardScreen(
    isVpnActive: Boolean,
    currentStage: EscalationStage,
    overrideCountToday: Int,
    onToggleVpn: () -> Unit,
    onShowDisable: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status indicator
        val statusColor = if (isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val statusText = if (isVpnActive) "Protected" else "Unprotected"

        Surface(
            shape = MaterialTheme.shapes.large,
            color = statusColor.copy(alpha = 0.15f),
            modifier = Modifier.size(180.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isVpnActive) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = statusText,
                    modifier = Modifier.size(64.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats card
        Card(modifier = Modifier.fillMaxWidth(0.85f)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Today's Stats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Interventions", overrideCountToday.toString())
                    StatItem("Current Level", stageLabel(currentStage))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // VPN toggle
        Button(
            onClick = {
                if (isVpnActive) onShowDisable() else onToggleVpn()
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isVpnActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isVpnActive) "Disable Khalawat" else "Start Protection")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun stageLabel(stage: EscalationStage): String = when (stage) {
    EscalationStage.STAGE_1 -> "1 — Gentle"
    EscalationStage.STAGE_2 -> "2 — Active"
    EscalationStage.STAGE_3 -> "3 — Hard Lock"
    EscalationStage.COOLING -> "Cooling"
}

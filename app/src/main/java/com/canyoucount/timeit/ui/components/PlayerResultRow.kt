package com.canyoucount.timeit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canyoucount.timeit.ui.theme.AccentGreen
import com.canyoucount.timeit.ui.theme.AccentRed
import com.canyoucount.timeit.ui.theme.SandAmber

@Composable
fun PlayerResultRow(
    playerName: String,
    playerTime: Double,
    delta: Double,
    avgDelta: Double? = null,
    isRoundWinner: Boolean,
    isEliminated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val absDelta = kotlin.math.abs(delta)
    val isGodOfTime = absDelta <= 0.1
    val isKeeperOfTime = !isGodOfTime && absDelta <= 0.2
    val isTimeWizard = !isGodOfTime && !isKeeperOfTime && absDelta <= 0.3

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isRoundWinner) "★ $playerName" else playerName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isRoundWinner) AccentGreen else MaterialTheme.colorScheme.onSurface
                )
                if (isEliminated) {
                    Text(
                        text = "💀 ELIMINATED",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentRed
                    )
                }
            }
            if (avgDelta != null) {
                Text(
                    text = "Avg: %.2fs".format(avgDelta),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "%.2fs".format(playerTime),
                style = MaterialTheme.typography.headlineSmall,
                color = SandAmber
            )
            Text(
                text = "%+.2fs".format(delta),
                style = MaterialTheme.typography.headlineSmall,
                color = if (absDelta > 1.0) AccentRed else AccentGreen
            )
        }
        if (isGodOfTime) {
            Text(
                text = "⚡ God of Time!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = AccentGreen
            )
        } else if (isKeeperOfTime) {
            Text(
                text = "🕰️ Keeper of Time!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = AccentGreen
            )
        } else if (isTimeWizard) {
            Text(
                text = "🧙 Time Wizard!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = SandAmber
            )
        }
    }
}

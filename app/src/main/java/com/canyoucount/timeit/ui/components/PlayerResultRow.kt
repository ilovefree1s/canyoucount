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
    wins: Int,
    isRoundWinner: Boolean,
    modifier: Modifier = Modifier
) {
    val absDelta = kotlin.math.abs(delta)
    val isGodOfTime = absDelta <= 0.1
    val isKeeperOfTime = !isGodOfTime && absDelta <= 0.2
    val isTimeWizard = !isGodOfTime && !isKeeperOfTime && absDelta <= 0.3
    val isNiceTryBaby = absDelta >= 2.0
    val isNiceTryGrandpa = !isNiceTryBaby && absDelta >= 1.0

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isRoundWinner) "★ $playerName" else playerName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isRoundWinner) AccentGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "%.2fs".format(playerTime),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "%+.2fs".format(delta),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Wins: $wins",
                style = MaterialTheme.typography.bodyLarge
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
        } else if (isNiceTryGrandpa) {
            Text(
                text = "👴 Nice try grandpa!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = AccentRed
            )
        } else if (isNiceTryBaby) {
            Text(
                text = "👶 Nice try lil baby!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = AccentRed
            )
        }
    }
}

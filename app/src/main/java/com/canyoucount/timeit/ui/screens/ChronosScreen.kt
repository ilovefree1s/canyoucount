package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.ui.theme.AccentGreen
import com.canyoucount.timeit.ui.theme.SandAmber

@Composable
fun ChronosScreen(
    chronosPlayerName: String,
    roundResults: List<RoundResult>,
    players: List<Player>,
    onSeeWinner: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚡",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "CHRONOS",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 52.sp,
                letterSpacing = 6.sp
            ),
            color = AccentGreen
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = chronosPlayerName,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = SandAmber
        )
        Text(
            text = "hit the target perfectly",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        if (roundResults.isNotEmpty()) {
            Text(
                text = "Round Results",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            roundResults.sortedBy { kotlin.math.abs(it.delta) }.forEach { result ->
                val player = players.find { it.id == result.playerId }
                val isPerfect = result.delta == 0.0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPerfect) "⚡ ${player?.name ?: "?"}" else player?.name ?: "?",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isPerfect) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isPerfect) AccentGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "%.2fs".format(result.playerTime),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isPerfect) "PERFECT" else "%+.2fs".format(result.delta),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isPerfect) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isPerfect) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onSeeWinner, modifier = Modifier.fillMaxWidth()) {
            Text("See Winner")
        }
    }
}

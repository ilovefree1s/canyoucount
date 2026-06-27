package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.ui.components.PlayerResultRow
import com.canyoucount.timeit.ui.theme.AccentGreen
import com.canyoucount.timeit.ui.theme.AccentRed

@Composable
fun ResultsScreen(
    players: List<Player>,
    roundResults: List<RoundResult>,
    allResults: List<RoundResult> = emptyList(),
    isHost: Boolean = true,
    isTimeBankMode: Boolean = false,
    onReady: (() -> Unit)? = null,
    onNextRound: () -> Unit,
    onEndGame: () -> Unit
) {
    val sortedResults = roundResults.sortedBy { kotlin.math.abs(it.delta) }
    val minAbsDelta: Double? = if (sortedResults.isEmpty()) null else kotlin.math.abs(sortedResults.first().delta)
    val readyCount = players.count { it.ready }
    val allReady = readyCount == players.size && players.isNotEmpty()
    var localReady by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Round Results", style = MaterialTheme.typography.headlineMedium)

        sortedResults.forEach { result ->
            val player = players.find { it.id == result.playerId }
            val playerAllResults = allResults.filter { it.playerId == result.playerId }
            val avgDelta = if (playerAllResults.isEmpty()) null
                else playerAllResults.map { kotlin.math.abs(it.delta) }.average()
            PlayerResultRow(
                playerName = player?.name ?: "Unknown",
                playerTime = result.playerTime,
                delta = result.delta,
                avgDelta = avgDelta,
                isRoundWinner = minAbsDelta != null && kotlin.math.abs(result.delta) == minAbsDelta
            )
            if (isTimeBankMode && player != null) {
                if (player.eliminated) {
                    Text(
                        text = "💀 ELIMINATED",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = AccentRed
                    )
                } else {
                    Text(
                        text = "Bank: %.2fs remaining".format(player.bank),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (player.bank < 0.3) AccentRed else AccentGreen
                    )
                }
            }
        }

        if (onReady != null) {
            Text(
                text = "Ready: $readyCount / ${players.size}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (allReady) AccentGreen else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onReady != null) {
                // Online mode: ready-up flow
                if (!localReady) {
                    Button(
                        onClick = {
                            localReady = true
                            onReady()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ready")
                    }
                } else {
                    Text(
                        text = "✓ You are ready",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentGreen
                    )
                }
            } else {
                // Local mode: host advances directly
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                    Text("Next Round")
                }
            }

            OutlinedButton(onClick = onEndGame, modifier = Modifier.fillMaxWidth()) {
                Text("End Game")
            }
        }
    }
}

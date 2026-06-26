package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.ui.components.PlayerResultRow

@Composable
fun ResultsScreen(
    players: List<Player>,
    roundResults: List<RoundResult>,
    isHost: Boolean = true,
    onNextRound: () -> Unit,
    onEndGame: () -> Unit
) {
    val sortedResults = roundResults.sortedBy { kotlin.math.abs(it.delta) }
    val minAbsDelta = sortedResults.firstOrNull()?.let { kotlin.math.abs(it.delta) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Round Results", style = MaterialTheme.typography.headlineMedium)

        sortedResults.forEach { result ->
            val player = players.find { it.id == result.playerId }
            PlayerResultRow(
                playerName = player?.name ?: "Unknown",
                playerTime = result.playerTime,
                delta = result.delta,
                wins = player?.wins ?: 0,
                isRoundWinner = kotlin.math.abs(result.delta) == minAbsDelta
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isHost) {
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                    Text("Next Round")
                }
            }
            Button(onClick = onEndGame, modifier = Modifier.fillMaxWidth()) {
                Text("End Game")
            }
        }
    }
}

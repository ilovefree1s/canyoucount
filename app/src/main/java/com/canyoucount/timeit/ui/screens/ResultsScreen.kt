package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.canyoucount.timeit.util.RoastMessages

@Composable
fun ResultsScreen(
    players: List<Player>,
    roundResults: List<RoundResult>,
    allResults: List<RoundResult> = emptyList(),
    isHost: Boolean = true,
    isTimeBankMode: Boolean = false,
    isSurvivalMode: Boolean = false,
    isTeamMode: Boolean = false,
    isGameOver: Boolean = false,
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

        val teamLabels = listOf("A", "B", "C", "D")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isTeamMode) {
                val teamIds = players.map { it.teamId }.distinct().sorted()
                // Best team = lowest avg delta this round
                val bestTeamId = teamIds.minByOrNull { tid ->
                    val teamResults = sortedResults.filter { res -> players.any { it.teamId == tid && it.id == res.playerId } }
                    if (teamResults.isEmpty()) Double.MAX_VALUE else teamResults.map { kotlin.math.abs(it.delta) }.average()
                }
                teamIds.forEach { teamId ->
                    val teamResults = sortedResults.filter { res -> players.any { it.teamId == teamId && it.id == res.playerId } }
                    val teamAvg = if (teamResults.isEmpty()) null else teamResults.map { kotlin.math.abs(it.delta) }.average()
                    val teamEliminated = isTimeBankMode && players.filter { it.teamId == teamId }.all { it.eliminated }
                    val isWinningTeam = teamId == bestTeamId

                    Text(
                        text = buildString {
                            append(if (isWinningTeam) "★ " else "")
                            append("Team ${teamLabels.getOrElse(teamId - 1) { "?" }}")
                            if (teamAvg != null) append("  —  %.2fs avg".format(teamAvg))
                            if (teamEliminated) append("  💀")
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isWinningTeam) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        color = if (teamEliminated) AccentRed else if (isWinningTeam) AccentGreen else MaterialTheme.colorScheme.onSurface
                    )
                    if (isTimeBankMode) {
                        val teamBank = players.filter { it.teamId == teamId }.minOfOrNull { it.bank } ?: 0.0
                        Text(
                            text = if (teamEliminated) "Bank depleted" else "Bank: %.2fs remaining".format(teamBank),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (teamEliminated || teamBank < 0.3) AccentRed else AccentGreen
                        )
                    }
                    teamResults.forEach { result ->
                        val player = players.find { it.id == result.playerId }
                        val absDelta = kotlin.math.abs(result.delta)
                        val roast = remember(result.playerId) { if (absDelta > 2.0) RoastMessages.random(result.targetTime, absDelta) else null }
                        PlayerResultRow(
                            playerName = player?.name ?: "Unknown",
                            playerTime = result.playerTime,
                            delta = result.delta,
                            isRoundWinner = false,
                            isEliminated = isTimeBankMode && player?.eliminated == true,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        if (roast != null) {
                            Text(
                                text = roast,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = AccentRed,
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            } else {
                sortedResults.forEach { result ->
                    val player = players.find { it.id == result.playerId }
                    val playerAllResults = allResults.filter { it.playerId == result.playerId }
                    val avgDelta = if (isSurvivalMode || playerAllResults.isEmpty()) null
                        else playerAllResults.map { kotlin.math.abs(it.delta) }.average()
                    val absDelta = kotlin.math.abs(result.delta)
                    val roast = remember(result.playerId) { if (!isSurvivalMode && absDelta > 2.0) RoastMessages.random(result.targetTime, absDelta) else null }
                    PlayerResultRow(
                        playerName = player?.name ?: "Unknown",
                        playerTime = result.playerTime,
                        delta = result.delta,
                        avgDelta = avgDelta,
                        isRoundWinner = minAbsDelta != null && kotlin.math.abs(result.delta) == minAbsDelta,
                        isEliminated = (isSurvivalMode || isTimeBankMode) && player?.eliminated == true
                    )
                    if (roast != null) {
                        Text(
                            text = roast,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = AccentRed,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (isTimeBankMode && player != null && !player.eliminated) {
                        Text(
                            text = "Bank: %.2fs remaining".format(player.bank),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (player.bank < 0.3) AccentRed else AccentGreen
                        )
                    }
                }
            }
        }

        if (onReady != null && !isGameOver) {
            Text(
                text = "Ready: $readyCount / ${players.size}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (allReady) AccentGreen else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onReady != null && !isGameOver) {
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
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isGameOver) "See Winner" else "Next Round")
                }
            }

            OutlinedButton(onClick = onEndGame, modifier = Modifier.fillMaxWidth()) {
                Text("End Game")
            }
        }
    }
}

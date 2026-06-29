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
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.ui.theme.AccentGreen
import com.canyoucount.timeit.ui.theme.AccentRed
import com.canyoucount.timeit.ui.theme.SandAmber

@Composable
fun WinnerScreen(
    winnerName: String,
    winnerAvgDelta: Double? = null,
    players: List<Player> = emptyList(),
    allResults: List<RoundResult> = emptyList(),
    roundResults: List<RoundResult> = emptyList(),
    isTimeBankMode: Boolean = false,
    isSurvivalMode: Boolean = false,
    isTeamMode: Boolean = false,
    winningTeamId: Int? = null,
    onRematch: (() -> Unit)? = null,
    rematchVoteCount: Int = 0,
    totalPlayers: Int = 0,
    hasVotedRematch: Boolean = false,
    onHome: () -> Unit
) {
    data class PlayerScore(val name: String, val stat: Double, val isWinner: Boolean)
    data class TeamScore(val teamId: Int, val label: String, val stat: Double, val isWinner: Boolean, val bank: Double?)

    val teamLabels = listOf("A", "B", "C", "D")

    val teamScores: List<TeamScore> = if (isTeamMode && players.isNotEmpty() && allResults.isNotEmpty()) {
        players.map { it.teamId }.distinct().sorted().map { tid ->
            val label = teamLabels.getOrElse(tid - 1) { "?" }
            val teamResults = allResults.filter { res -> players.any { it.teamId == tid && it.id == res.playerId } }
            val stat = if (teamResults.isEmpty()) Double.MAX_VALUE
                       else if (isSurvivalMode) teamResults.minOf { kotlin.math.abs(it.delta) }
                       else teamResults.map { kotlin.math.abs(it.delta) }.average()
            val bank = if (isTimeBankMode) players.filter { it.teamId == tid }.minOfOrNull { it.bank } else null
            TeamScore(tid, label, stat, tid == winningTeamId, bank)
        }.sortedBy { it.stat }
    } else emptyList()

    val scores = if (!isTeamMode && players.isNotEmpty() && allResults.isNotEmpty()) {
        players.map { player ->
            val results = allResults.filter { it.playerId == player.id }
            val stat = if (results.isEmpty()) Double.MAX_VALUE
                       else if (isSurvivalMode) results.minOf { kotlin.math.abs(it.delta) }
                       else results.map { kotlin.math.abs(it.delta) }.average()
            PlayerScore(player.name, stat, player.name == winnerName)
        }.sortedBy { it.stat }
    } else emptyList()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🏆", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "$winnerName wins!",
            style = MaterialTheme.typography.headlineMedium,
            color = SandAmber
        )

        if (isTimeBankMode && roundResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Last Round",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            roundResults.sortedBy { kotlin.math.abs(it.delta) }.forEach { result ->
                val player = players.find { it.id == result.playerId }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "%.2fs".format(result.playerTime),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val deltaSign = if (result.delta >= 0) "+" else ""
                        Text(
                            text = "$deltaSign%.2fs".format(result.delta),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (kotlin.math.abs(result.delta) == roundResults.minOf { kotlin.math.abs(it.delta) }) AccentGreen else AccentRed
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }

        if (teamScores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Final Scores",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            teamScores.forEachIndexed { index, score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (score.isWinner) "★ Team ${score.label}" else "Team ${score.label}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (score.isWinner) FontWeight.Bold else FontWeight.Normal),
                            color = if (score.isWinner) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (isTimeBankMode && score.bank != null) "%.2fs bank".format(score.bank)
                               else if (isSurvivalMode) "%.2fs best timeit".format(score.stat)
                               else "%.2fs avg".format(score.stat),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            score.isWinner -> AccentGreen
                            index == teamScores.lastIndex -> AccentRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                if (index < teamScores.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
            HorizontalDivider()
        }

        if (scores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Final Scores",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            scores.forEachIndexed { index, score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (score.isWinner) "★ ${score.name}" else score.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (score.isWinner) FontWeight.Bold else FontWeight.Normal),
                            color = if (score.isWinner) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (isSurvivalMode) "%.2fs best timeit".format(score.stat) else "%.2fs avg".format(score.stat),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            score.isWinner -> AccentGreen
                            index == scores.lastIndex -> AccentRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                if (index < scores.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
            HorizontalDivider()
        }

        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (onRematch != null) {
                val isOnline = totalPlayers > 0
                if (!hasVotedRematch) {
                    Button(onClick = onRematch, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isOnline) "Rematch ($rematchVoteCount/$totalPlayers)" else "Rematch")
                    }
                } else {
                    Text(
                        text = if (rematchVoteCount >= totalPlayers) "Starting rematch…"
                               else "Waiting for others ($rematchVoteCount/$totalPlayers)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentGreen
                    )
                }
            }
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        }
    }
}

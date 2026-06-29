package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.GameConfig

private val TEAM_LABELS = listOf("A", "B", "C", "D")

@Composable
fun LobbyScreen(
    onStartGame: (List<Pair<String, Int>>, GameConfig) -> Unit
) {
    val playerNames = remember { mutableStateOf(listOf("Player 1", "Player 2")) }
    var winTarget by remember { mutableStateOf(3) }
    var winTargetMenuExpanded by remember { mutableStateOf(false) }
    var gameMode by remember { mutableStateOf("standard") }
    var timeBankSeconds by remember { mutableDoubleStateOf(1.0) }
    var isTeamMode by remember { mutableStateOf(false) }
    var teamCount by remember { mutableStateOf(2) }
    val playerTeamIds = remember { mutableStateOf(listOf(1, 2)) }

    fun ensureTeamIds() {
        val size = playerNames.value.size
        val current = playerTeamIds.value
        playerTeamIds.value = List(size) { i ->
            if (i < current.size) current[i] else ((i % teamCount) + 1)
        }
    }

    fun reassignTeams() {
        playerTeamIds.value = playerNames.value.mapIndexed { i, _ -> (i % teamCount) + 1 }
    }

    val teamsValid = !isTeamMode || (1..teamCount).all { id -> playerTeamIds.value.any { it == id } }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Pass and Play", style = MaterialTheme.typography.headlineMedium)

        // Team mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Team Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isTeamMode,
                onCheckedChange = {
                    isTeamMode = it
                    if (it) reassignTeams() else playerTeamIds.value = List(playerNames.value.size) { 0 }
                }
            )
        }

        if (isTeamMode) {
            Text("Number of teams", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4).forEach { n ->
                    FilterChip(
                        selected = teamCount == n,
                        onClick = { teamCount = n; reassignTeams() },
                        label = { Text("$n") }
                    )
                }
            }
        }

        HorizontalDivider()

        // Player names + team assignment
        playerNames.value.forEachIndexed { index, name ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        playerNames.value = playerNames.value.toMutableList().also { it[index] = newName }
                    },
                    label = { Text("Player ${index + 1}") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isTeamMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Team:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        (1..teamCount).forEach { teamId ->
                            FilterChip(
                                selected = playerTeamIds.value.getOrElse(index) { 1 } == teamId,
                                onClick = {
                                    val updated = playerTeamIds.value.toMutableList()
                                    while (updated.size <= index) updated.add(1)
                                    updated[index] = teamId
                                    playerTeamIds.value = updated
                                },
                                label = { Text(TEAM_LABELS[teamId - 1], fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        if (playerNames.value.size < 8) {
            Button(onClick = {
                playerNames.value = playerNames.value + "Player ${playerNames.value.size + 1}"
                ensureTeamIds()
            }) { Text("Add Player") }
        }

        HorizontalDivider()

        Text(text = "Game mode", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = gameMode == "standard", onClick = { gameMode = "standard" }, label = { Text("Standard") })
            FilterChip(selected = gameMode == "timebank", onClick = { gameMode = "timebank" }, label = { Text("Time Bank") })
            if (!isTeamMode) {
                FilterChip(selected = gameMode == "survival", onClick = { gameMode = "survival" }, label = { Text("Survival") })
            } else if (gameMode == "survival") {
                gameMode = "standard"
            }
        }

        when (gameMode) {
            "standard" -> Box {
                Button(onClick = { winTargetMenuExpanded = true }) { Text("Rounds: $winTarget") }
                DropdownMenu(expanded = winTargetMenuExpanded, onDismissRequest = { winTargetMenuExpanded = false }) {
                    listOf(3, 5, 7, 10).forEach { option ->
                        DropdownMenuItem(text = { Text(option.toString()) }, onClick = {
                            winTarget = option
                            winTargetMenuExpanded = false
                        })
                    }
                }
            }
            "timebank" -> {
                Text(text = "Starting bank", style = MaterialTheme.typography.bodyLarge)
                if (isTeamMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2.0, 3.0, 4.0, 5.0, 6.0).forEach { option ->
                            FilterChip(selected = timeBankSeconds == option, onClick = { timeBankSeconds = option }, label = { Text("${option.toInt()}s") })
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1.0, 2.0, 3.0, 4.0).forEach { option ->
                            FilterChip(selected = timeBankSeconds == option, onClick = { timeBankSeconds = option }, label = { Text("${option.toInt()}s") })
                        }
                    }
                }
                Text(
                    text = if (isTeamMode)
                        "The worst miss each round drains the whole team's bank. Last team with bank remaining wins!"
                    else
                        "Miss the target and lose that time from your bank. Last one with bank remaining wins!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            "survival" -> Text(
                text = "Go over the target time or be the farthest away — you're eliminated. Last player standing wins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (isTeamMode && !teamsValid) {
            Text(
                text = "Each team needs at least one player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val pairs = playerNames.value.mapIndexed { i, name ->
                    name to (if (isTeamMode) playerTeamIds.value.getOrElse(i) { 1 } else 0)
                }.filter { it.first.isNotBlank() }
                onStartGame(
                    pairs,
                    GameConfig(
                        winTarget = winTarget,
                        gameMode = gameMode,
                        timeBankSeconds = timeBankSeconds,
                        isTeamMode = isTeamMode,
                        teamCount = teamCount
                    )
                )
            },
            enabled = playerNames.value.count { it.isNotBlank() } >= 2 && teamsValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Game")
        }
    }
}

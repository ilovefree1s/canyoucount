package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.GameConfig

@Composable
fun LobbyScreen(
    onStartGame: (List<String>, GameConfig) -> Unit
) {
    val playerNames = remember { mutableStateOf(listOf("Player 1", "Player 2")) }
    var winTarget by remember { mutableStateOf(3) }
    var winTargetMenuExpanded by remember { mutableStateOf(false) }
    var gameMode by remember { mutableStateOf("standard") }
    var timeBankSeconds by remember { mutableDoubleStateOf(1.0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Pass and Play", style = MaterialTheme.typography.headlineMedium)

        playerNames.value.forEachIndexed { index, name ->
            OutlinedTextField(
                value = name,
                onValueChange = { newName ->
                    playerNames.value = playerNames.value.toMutableList().also { it[index] = newName }
                },
                label = { Text("Player ${index + 1}") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (playerNames.value.size < 8) {
            Button(onClick = {
                playerNames.value = playerNames.value + "Player ${playerNames.value.size + 1}"
            }) { Text("Add Player") }
        }

        Text(text = "Game mode", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = gameMode == "standard",
                onClick = { gameMode = "standard" },
                label = { Text("Standard") }
            )
            FilterChip(
                selected = gameMode == "timebank",
                onClick = { gameMode = "timebank" },
                label = { Text("Time Bank") }
            )
        }

        if (gameMode == "standard") {
            Box {
                Button(onClick = { winTargetMenuExpanded = true }) {
                    Text("Win target: $winTarget round(s)")
                }
                DropdownMenu(expanded = winTargetMenuExpanded, onDismissRequest = { winTargetMenuExpanded = false }) {
                    listOf(1, 3, 5, 7, 10).forEach { option ->
                        DropdownMenuItem(text = { Text(option.toString()) }, onClick = {
                            winTarget = option
                            winTargetMenuExpanded = false
                        })
                    }
                }
            }
        } else {
            Text(text = "Starting bank", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0, 1.5, 2.0, 2.5, 3.0).forEach { option ->
                    FilterChip(
                        selected = timeBankSeconds == option,
                        onClick = { timeBankSeconds = option },
                        label = { Text("${option}s") }
                    )
                }
            }
            Text(
                text = "Miss the target and lose that time from your bank. Last one with bank remaining wins!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Button(
            onClick = {
                onStartGame(
                    playerNames.value.filter { it.isNotBlank() },
                    GameConfig(winTarget = winTarget, gameMode = gameMode, timeBankSeconds = timeBankSeconds)
                )
            },
            enabled = playerNames.value.count { it.isNotBlank() } >= 2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Game")
        }
    }
}

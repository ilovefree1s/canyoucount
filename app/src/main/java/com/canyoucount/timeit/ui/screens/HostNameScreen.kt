package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HostNameScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    errorMessage: String? = null,
    onHost: (winTarget: Int, gameMode: String, timeBankSeconds: Double) -> Unit
) {
    var winTarget by remember { mutableIntStateOf(3) }
    var gameMode by remember { mutableStateOf("standard") }
    var timeBankSeconds by remember { mutableDoubleStateOf(1.0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Host a Game", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = playerName,
            onValueChange = onPlayerNameChange,
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth()
        )

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
            Text(text = "Rounds to win", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5, 7).forEach { option ->
                    FilterChip(
                        selected = winTarget == option,
                        onClick = { winTarget = option },
                        label = { Text("$option") }
                    )
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
                text = "Miss the target and lose that time. Last one with bank remaining wins!",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onHost(winTarget, gameMode, timeBankSeconds) },
            enabled = playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Room")
        }
    }
}

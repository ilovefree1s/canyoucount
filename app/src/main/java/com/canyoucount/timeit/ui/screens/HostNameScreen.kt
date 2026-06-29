package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
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
    onHost: (winTarget: Int, gameMode: String, timeBankSeconds: Double, isTeamMode: Boolean, teamCount: Int) -> Unit
) {
    var winTarget by remember { mutableIntStateOf(3) }
    var gameMode by remember { mutableStateOf("standard") }
    var timeBankSeconds by remember { mutableDoubleStateOf(1.0) }
    var isTeamMode by remember { mutableStateOf(false) }
    var teamCount by remember { mutableIntStateOf(2) }

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

        // Team mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Team Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isTeamMode, onCheckedChange = { isTeamMode = it; if (it && gameMode == "survival") gameMode = "standard" })
        }

        if (isTeamMode) {
            Text("Number of teams", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4).forEach { n ->
                    FilterChip(selected = teamCount == n, onClick = { teamCount = n }, label = { Text("$n") })
                }
            }
        }

        Text(text = "Game mode", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = gameMode == "standard", onClick = { gameMode = "standard" }, label = { Text("Standard") })
            FilterChip(selected = gameMode == "timebank", onClick = { gameMode = "timebank" }, label = { Text("Time Bank") })
            if (!isTeamMode) {
                FilterChip(selected = gameMode == "survival", onClick = { gameMode = "survival" }, label = { Text("Survival") })
            }
        }

        when (gameMode) {
            "standard" -> {
                Text(text = "Rounds to win", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5, 7).forEach { option ->
                        FilterChip(selected = winTarget == option, onClick = { winTarget = option }, label = { Text("$option") })
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
                    text = if (isTeamMode) "The worst miss each round drains the whole team's bank. Last team with bank remaining wins!"
                           else "Miss the target and lose that time. Last one with bank remaining wins!",
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

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onHost(winTarget, gameMode, timeBankSeconds, isTeamMode, teamCount) },
            enabled = playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Room")
        }
    }
}

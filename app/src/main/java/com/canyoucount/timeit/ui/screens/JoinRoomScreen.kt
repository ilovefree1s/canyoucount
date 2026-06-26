package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JoinRoomScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    isJoining: Boolean,
    errorMessage: String?,
    onJoinRoom: (roomCode: String) -> Unit
) {
    var roomCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Join a Game", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = playerName,
            onValueChange = onPlayerNameChange,
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = roomCode,
            onValueChange = { roomCode = it.uppercase().take(6) },
            label = { Text("Room code") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onJoinRoom(roomCode) },
            enabled = !isJoining && roomCode.length == 6 && playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isJoining) "Joining..." else "Join Room")
        }
    }
}

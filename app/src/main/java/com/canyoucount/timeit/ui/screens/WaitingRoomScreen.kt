package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.ui.theme.SandAmber

@Composable
fun WaitingRoomScreen(
    roomCode: String,
    players: List<Player>,
    isHost: Boolean,
    onStartGame: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Room Code", style = MaterialTheme.typography.bodyLarge)
        Text(text = roomCode, style = MaterialTheme.typography.displayLarge, color = SandAmber)

        Text(text = "Players (${players.size})", style = MaterialTheme.typography.headlineMedium)
        LazyColumn {
            items(players) { player ->
                Text(text = player.name, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (isHost) {
            Button(
                onClick = onStartGame,
                enabled = players.size >= 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Game")
            }
        } else {
            Text(text = "Waiting for host to start…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

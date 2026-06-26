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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.ui.theme.SandAmber

@Composable
fun WinnerScreen(
    winnerName: String,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
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

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onPlayAgain) { Text("Play Again") }
            Button(onClick = onHome) { Text("Home") }
        }
    }
}

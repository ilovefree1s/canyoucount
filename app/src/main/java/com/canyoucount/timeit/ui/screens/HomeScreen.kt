package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onSinglePlayer: () -> Unit,
    onPassAndPlay: () -> Unit,
    onPlayOnline: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        val titleOffset = maxHeight * 0.20f
        val buttonsOffset = maxHeight * 0.10f

        Column(
            modifier = Modifier.offset(y = -titleOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "TimeIt", style = MaterialTheme.typography.displayLarge)
            Text(text = "Can you even count bro?", style = MaterialTheme.typography.bodyLarge)
        }

        Column(
            modifier = Modifier.offset(y = buttonsOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onSinglePlayer, modifier = Modifier.fillMaxWidth()) { Text("Solo") }
            Button(onClick = onPassAndPlay, modifier = Modifier.fillMaxWidth()) { Text("Pass and Play") }
            Button(onClick = onPlayOnline, modifier = Modifier.fillMaxWidth()) { Text("Play Online") }
        }

        Text(
            text = "created by Justin Hamilton",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

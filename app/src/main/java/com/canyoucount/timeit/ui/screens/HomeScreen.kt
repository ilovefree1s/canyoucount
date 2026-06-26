package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onPassAndPlay: () -> Unit,
    onPlayOnline: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "TimeIt", style = MaterialTheme.typography.displayLarge)
        Text(text = "How precisely can you feel time?", style = MaterialTheme.typography.bodyLarge)

        Column(
            modifier = Modifier.padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onPassAndPlay) { Text("Pass and Play") }
            Button(onClick = onPlayOnline) { Text("Play Online") }
        }
    }
}

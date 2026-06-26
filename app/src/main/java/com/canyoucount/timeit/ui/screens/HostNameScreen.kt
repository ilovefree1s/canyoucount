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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HostNameScreen(
    playerName: String,
    onPlayerNameChange: (String) -> Unit,
    errorMessage: String? = null,
    onHost: () -> Unit
) {
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

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onHost,
            enabled = playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Room")
        }
    }
}

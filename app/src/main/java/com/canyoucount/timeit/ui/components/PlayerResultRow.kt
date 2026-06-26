package com.canyoucount.timeit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.ui.theme.AccentGreen

@Composable
fun PlayerResultRow(
    playerName: String,
    playerTime: Double,
    delta: Double,
    wins: Int,
    isRoundWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isRoundWinner) "★ $playerName" else playerName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isRoundWinner) AccentGreen else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "%.2fs".format(playerTime),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "%+.2fs".format(delta),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Wins: $wins",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

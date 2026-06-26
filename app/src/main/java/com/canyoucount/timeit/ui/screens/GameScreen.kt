package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.ui.components.CountdownDisplay
import com.canyoucount.timeit.ui.components.HourglassAnimation
import com.canyoucount.timeit.viewmodel.GamePhase
import com.canyoucount.timeit.viewmodel.GameViewModel
import kotlinx.coroutines.delay

/**
 * Drives the four GameScreen sub-states for pass-and-play: target reveal,
 * countdown, tapping, then hands off to ResultsScreen / WinnerScreen via navigation.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onRoundFinished: () -> Unit
) {
    val phase by viewModel.phase.collectAsState()
    val targetTime by viewModel.targetTime.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val currentPlayerIndex by viewModel.currentPlayerIndex.collectAsState()

    LaunchedEffect(phase) {
        if (phase == GamePhase.Results || phase == GamePhase.Winner) {
            onRoundFinished()
        }
    }

    val currentPlayerName = gameState.players.getOrNull(currentPlayerIndex)?.name ?: ""

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (phase) {
            GamePhase.TargetReveal -> TargetRevealContent(
                targetTime = targetTime,
                playerName = currentPlayerName,
                onFinished = viewModel::onTargetRevealFinished
            )
            GamePhase.Countdown -> CountdownDisplay(onFinished = viewModel::onCountdownFinished)
            GamePhase.Tapping -> TappingContent(onTap = viewModel::onPlayerTap)
            else -> Unit
        }
    }
}

@Composable
private fun TargetRevealContent(
    targetTime: Double,
    playerName: String,
    onFinished: () -> Unit
) {
    LaunchedEffect(targetTime, playerName) {
        delay(3000)
        onFinished()
    }
    Column(
        modifier = Modifier.fillMaxSize().clickable { onFinished() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (playerName.isNotBlank()) {
            Text(text = "$playerName's turn", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            text = "%.2f".format(targetTime),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(text = "Remember this time", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TappingContent(onTap: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().clickable { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HourglassAnimation(frozen = false)
        Text(
            text = "Tap anywhere to stop",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.ui.components.CountdownDisplay
import com.canyoucount.timeit.ui.components.HourglassAnimation
import com.canyoucount.timeit.viewmodel.GamePhase
import kotlinx.coroutines.delay

/**
 * Online-mode equivalent of GameScreen: target reveal, countdown, tapping, and
 * the post-tap "waiting for others" state, all driven by the room's Realtime-observed
 * `status`/`target_time` columns (see OnlineGameViewModel.observeRoom).
 */
@Composable
fun OnlineGameScreen(
    phase: GamePhase,
    targetTime: Double,
    onTargetRevealFinished: () -> Unit,
    onCountdownFinished: () -> Unit,
    onTap: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (phase) {
            GamePhase.TargetReveal -> TargetRevealContent(
                targetTime = targetTime,
                onFinished = onTargetRevealFinished
            )

            GamePhase.Countdown -> CountdownDisplay(onFinished = onCountdownFinished)

            GamePhase.Tapping -> Column(
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

            GamePhase.Waiting -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Waiting for other players…",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            else -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Get ready…", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun TargetRevealContent(
    targetTime: Double,
    onFinished: () -> Unit
) {
    LaunchedEffect(targetTime) {
        delay(3000)
        onFinished()
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "%.2f".format(targetTime),
            style = MaterialTheme.typography.displayLarge
        )
        Text(text = "Remember this time", style = MaterialTheme.typography.bodyLarge)
    }
}

package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.ui.components.CountdownDisplay
import com.canyoucount.timeit.ui.theme.AccentGreen
import com.canyoucount.timeit.ui.theme.AccentRed
import com.canyoucount.timeit.ui.theme.SandAmber
import com.canyoucount.timeit.util.RoastMessages
import com.canyoucount.timeit.util.SoloLeaderboardStore
import com.canyoucount.timeit.viewmodel.GamePhase
import com.canyoucount.timeit.viewmodel.SinglePlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun SinglePlayerScreen(
    viewModel: SinglePlayerViewModel,
    onHome: () -> Unit
) {
    val phase by viewModel.phase.collectAsState()
    val targetTime by viewModel.targetTime.collectAsState()
    val currentRound by viewModel.currentRound.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val allResults by viewModel.allResults.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val bank by viewModel.bank.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (phase) {
            GamePhase.Setup -> SoloModeSelect(
                onSelectMode = { mode, bank, rounds -> viewModel.startGame(mode, bank, rounds) },
                onHome = onHome
            )
            GamePhase.TargetReveal -> SoloTargetReveal(
                round = currentRound,
                targetTime = targetTime,
                isTimeBankMode = gameMode == "timebank",
                bank = bank,
                onFinished = viewModel::onTargetRevealFinished
            )
            GamePhase.Countdown -> CountdownDisplay(
                onGo = viewModel::onCountdownGo,
                onFinished = viewModel::onCountdownFinished
            )
            GamePhase.Tapping -> Column(
                modifier = Modifier.fillMaxSize().clickable { viewModel.onTap() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tap anywhere to stop",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            GamePhase.RoundResult -> lastResult?.let { result ->
                SoloRoundResult(
                    round = currentRound,
                    totalRounds = viewModel.getTotalRounds,
                    result = result,
                    isTimeBankMode = gameMode == "timebank",
                    bank = bank,
                    onNext = viewModel::onNextRound
                )
            }
            GamePhase.Results -> SoloFinalResults(
                results = allResults,
                onPlayAgain = viewModel::restart,
                onHome = onHome
            )
            GamePhase.GameOver -> SoloGameOver(
                results = allResults,
                onPlayAgain = viewModel::restart,
                onHome = onHome
            )
            else -> Unit
        }
    }
}

@Composable
private fun SoloModeSelect(onSelectMode: (String, Double, Int) -> Unit, onHome: () -> Unit) {
    var gameMode by remember { mutableStateOf("standard") }
    var bankSeconds by remember { mutableDoubleStateOf(1.0) }
    var roundsMenuExpanded by remember { mutableStateOf(false) }
    var rounds by remember { mutableStateOf(3) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val standardScores by SoloLeaderboardStore.standardScores(context).collectAsState(initial = emptyList())
    val timeBankScores by SoloLeaderboardStore.timeBankScores(context).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Solo Play", style = MaterialTheme.typography.headlineMedium)

        Text(text = "Game mode", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = gameMode == "standard", onClick = { gameMode = "standard" }, label = { Text("Standard") })
            FilterChip(selected = gameMode == "timebank", onClick = { gameMode = "timebank" }, label = { Text("Time Bank") })
        }

        when (gameMode) {
            "standard" -> Box {
                Button(onClick = { roundsMenuExpanded = true }) { Text("Rounds: $rounds") }
                DropdownMenu(expanded = roundsMenuExpanded, onDismissRequest = { roundsMenuExpanded = false }) {
                    listOf(3, 5, 7, 10).forEach { option ->
                        DropdownMenuItem(text = { Text(option.toString()) }, onClick = {
                            rounds = option
                            roundsMenuExpanded = false
                        })
                    }
                }
            }
            "timebank" -> {
                Text(text = "Starting bank", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1.0, 2.0, 3.0, 4.0).forEach { option ->
                        FilterChip(selected = bankSeconds == option, onClick = { bankSeconds = option }, label = { Text("${option.toInt()}s") })
                    }
                }
                Text(
                    text = "Each round your error is deducted from your bank. Bank hits zero — game over.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Button(
            onClick = { onSelectMode(gameMode, bankSeconds, rounds) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Game")
        }

        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }

        // Leaderboard
        if (gameMode == "standard" && standardScores.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider()
            Text(text = "Best Scores", style = MaterialTheme.typography.titleMedium, color = SandAmber)
            standardScores.take(5).forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "%.2fs avg".format(entry.avgError),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (index == 0) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${entry.rounds}R",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (gameMode == "timebank") {
            val hasAny = listOf(1.0, 2.0, 3.0, 4.0).any { bank -> timeBankScores.any { it.startBank == bank } }
            if (hasAny) {
                androidx.compose.material3.HorizontalDivider()
                Text(text = "Best Scores", style = MaterialTheme.typography.titleMedium, color = SandAmber)
                listOf(1.0, 2.0, 3.0, 4.0).forEach { bank ->
                    val group = timeBankScores.filter { it.startBank == bank }
                    if (group.isNotEmpty()) {
                        Text(
                            text = "${bank.toInt()}s bank",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        group.take(5).forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "${entry.rounds} round${if (entry.rounds != 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (index == 0) AccentGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "%.2fs avg".format(entry.avgError),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoloTargetReveal(
    round: Int,
    targetTime: Double,
    isTimeBankMode: Boolean,
    bank: Double,
    onFinished: () -> Unit
) {
    LaunchedEffect(targetTime) {
        delay(3000)
        onFinished()
    }
    Column(
        modifier = Modifier.fillMaxSize().clickable { onFinished() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isTimeBankMode) {
            Text(
                text = "Round $round",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Bank: %.2fs".format(bank),
                style = MaterialTheme.typography.bodyLarge,
                color = if (bank < 0.3) AccentRed else AccentGreen,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(text = "Round $round of 3", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            text = "%.2f".format(targetTime),
            style = MaterialTheme.typography.displayLarge,
            color = SandAmber,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(text = "Remember this time", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Tap to continue",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

@Composable
private fun SoloRoundResult(
    round: Int,
    totalRounds: Int,
    result: RoundResult,
    isTimeBankMode: Boolean = false,
    bank: Double = 0.0,
    onNext: () -> Unit
) {
    val absDelta = kotlin.math.abs(result.delta)
    val roast = remember(result.playerTime) { if (absDelta > 2.0) RoastMessages.random(result.targetTime, absDelta) else null }
    val (label, labelColor) = when {
        absDelta <= 0.1 -> "⚡ God of Time!" to AccentGreen
        absDelta <= 0.2 -> "🕰️ Keeper of Time!" to AccentGreen
        absDelta <= 0.3 -> "🧙 Time Wizard!" to SandAmber
        else -> null to SandAmber
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isTimeBankMode) "Round $round" else "Round $round of $totalRounds",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Target", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "%.2fs".format(result.targetTime),
            style = MaterialTheme.typography.displayLarge,
            color = SandAmber
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Your time", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "%.2fs".format(result.playerTime),
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "%+.2fs".format(result.delta),
            style = MaterialTheme.typography.headlineMedium,
            color = if (absDelta <= 0.3) AccentGreen else MaterialTheme.colorScheme.onSurface
        )
        if (roast != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = roast,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = AccentRed
            )
        } else if (label != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = labelColor
            )
        }
        if (isTimeBankMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bank remaining: %.2fs".format(bank),
                style = MaterialTheme.typography.headlineSmall,
                color = if (bank < 0.3) AccentRed else AccentGreen
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (isTimeBankMode) "Next Round"
                else if (round < totalRounds) "Next Round"
                else "See Results"
            )
        }
    }
}

@Composable
private fun SoloGameOver(
    results: List<RoundResult>,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val roundsSurvived = (results.size - 1).coerceAtLeast(0)
    val avgDelta = if (results.isEmpty()) 0.0 else results.map { kotlin.math.abs(it.delta) }.average()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bank Empty!",
            style = MaterialTheme.typography.displayMedium,
            color = AccentRed,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You survived $roundsSurvived round${if (roundsSurvived != 1) "s" else ""}",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        results.forEachIndexed { index, result ->
            val absDelta = kotlin.math.abs(result.delta)
            val rowColor = if (absDelta <= 0.3) AccentGreen else MaterialTheme.colorScheme.onSurface
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Round ${index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(16.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Target %.2fs".format(result.targetTime),
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(110.dp)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(16.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "%+.2fs".format(result.delta),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (kotlin.math.abs(result.delta) > 1.0) AccentRed else AccentGreen,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Avg error: %.2fs".format(avgDelta),
            style = MaterialTheme.typography.headlineSmall,
            color = SandAmber
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Play Again")
        }
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("Home")
        }
    }
}

@Composable
private fun SoloFinalResults(
    results: List<RoundResult>,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val avgDelta = if (results.isEmpty()) 0.0 else results.map { kotlin.math.abs(it.delta) }.average()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Final Results", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        results.forEachIndexed { index, result ->
            val absDelta = kotlin.math.abs(result.delta)
            val rowColor = if (absDelta <= 0.3) AccentGreen else MaterialTheme.colorScheme.onSurface
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Round ${index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(16.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Target %.2fs".format(result.targetTime),
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(110.dp)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = rowColor,
                    modifier = Modifier.width(16.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "%+.2fs".format(result.delta),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (kotlin.math.abs(result.delta) > 1.0) AccentRed else AccentGreen,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Average error: %.2fs".format(avgDelta),
            style = MaterialTheme.typography.headlineMedium,
            color = SandAmber
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Play Again")
        }
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text("Home")
        }
    }
}

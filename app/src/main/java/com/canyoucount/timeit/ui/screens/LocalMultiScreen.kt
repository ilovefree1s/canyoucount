package com.canyoucount.timeit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.ui.theme.AccentGreen
import com.canyoucount.timeit.ui.theme.AccentRed
import com.canyoucount.timeit.ui.theme.SandAmber
import com.canyoucount.timeit.util.TimerUtil
import com.canyoucount.timeit.viewmodel.GamePhase
import com.canyoucount.timeit.viewmodel.LocalMultiPlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun LocalMultiScreen(
    viewModel: LocalMultiPlayerViewModel,
    onHome: () -> Unit
) {
    val phase by viewModel.phase.collectAsState()
    val players by viewModel.players.collectAsState()
    val targetTime by viewModel.targetTime.collectAsState()
    val tapTimes by viewModel.tapTimes.collectAsState()
    val playerStartNanos by viewModel.playerStartNanos.collectAsState()
    val currentRound by viewModel.currentRound.collectAsState()
    val lastRoundResults by viewModel.lastRoundResults.collectAsState()
    val allResults by viewModel.allResults.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val chronosPlayerName by viewModel.chronosPlayerName.collectAsState()

    when (phase) {
        GamePhase.Setup -> LocalMultiSetup(
            onStart = { pairs, config -> viewModel.startGame(pairs, config) },
            onHome = onHome
        )
        GamePhase.TargetReveal -> LocalMultiTargetReveal(
            round = currentRound,
            targetTime = targetTime,
            onFinished = viewModel::onTargetRevealFinished
        )
        GamePhase.Tapping -> LocalMultiTapping(
            players = players,
            playerStartNanos = playerStartNanos,
            tapTimes = tapTimes,
            targetTime = targetTime,
            onTap = viewModel::onPlayerTap
        )
        GamePhase.RoundResult -> LocalMultiRoundResult(
            players = players,
            results = lastRoundResults,
            allResults = allResults,
            isTimeBankMode = gameMode == "timebank",
            isSurvivalMode = gameMode == "survival",
            isTeamMode = players.any { it.teamId != 0 },
            isGameOver = isGameOver,
            onNext = viewModel::nextRound,
            onSeeWinner = viewModel::goToWinner,
            onHome = onHome
        )
        GamePhase.Chronos -> ChronosScreen(
            chronosPlayerName = chronosPlayerName ?: "",
            roundResults = lastRoundResults,
            players = players,
            onSeeWinner = viewModel::goToWinner
        )
        GamePhase.Winner -> LocalMultiWinner(
            players = players,
            allResults = allResults,
            isTimeBankMode = gameMode == "timebank",
            isSurvivalMode = gameMode == "survival",
            isTeamMode = players.any { it.teamId != 0 },
            onPlayAgain = viewModel::restart,
            onHome = onHome
        )
        else -> {}
    }
}

private val TEAM_LABELS_MULTI = listOf("A", "B", "C", "D")

@Composable
private fun LocalMultiSetup(
    onStart: (List<Pair<String, Int>>, GameConfig) -> Unit,
    onHome: () -> Unit
) {
    var playerCount by remember { mutableIntStateOf(2) }
    var names by remember(playerCount) { mutableStateOf(List(playerCount) { index -> "Player ${index + 1}" }) }
    var rounds by remember { mutableIntStateOf(3) }
    var gameMode by remember { mutableStateOf("standard") }
    var bankSeconds by remember { mutableStateOf(1.0) }
    var isTeamMode by remember { mutableStateOf(false) }
    var teamCount by remember { mutableIntStateOf(2) }
    var teamIds by remember(playerCount) { mutableStateOf(List(playerCount) { i -> (i % 2) + 1 }) }

    fun reassignTeams() { teamIds = List(playerCount) { i -> (i % teamCount) + 1 } }

    val teamsValid = !isTeamMode || (1..teamCount).all { id -> teamIds.any { it == id } }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Screen Share", style = MaterialTheme.typography.headlineMedium)

        Text("Players", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4).forEach { count ->
                FilterChip(
                    selected = playerCount == count,
                    onClick = {
                        playerCount = count
                        teamIds = List(count) { i -> (i % teamCount) + 1 }
                    },
                    label = { Text("$count players") }
                )
            }
        }

        // Team mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Team Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isTeamMode,
                onCheckedChange = {
                    isTeamMode = it
                    if (it) reassignTeams() else teamIds = List(playerCount) { 0 }
                }
            )
        }

        if (isTeamMode) {
            Text("Number of teams", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4).forEach { n ->
                    FilterChip(selected = teamCount == n, onClick = { teamCount = n; reassignTeams() }, label = { Text("$n") })
                }
            }
        }

        HorizontalDivider()

        names.forEachIndexed { index, name ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { new -> names = names.toMutableList().also { it[index] = new } },
                    label = { Text("Player ${index + 1} name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isTeamMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Team:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        (1..teamCount).forEach { tid ->
                            FilterChip(
                                selected = teamIds.getOrElse(index) { 1 } == tid,
                                onClick = {
                                    val updated = teamIds.toMutableList()
                                    while (updated.size <= index) updated.add(1)
                                    updated[index] = tid
                                    teamIds = updated
                                },
                                label = { Text(TEAM_LABELS_MULTI[tid - 1], fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Mode", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = gameMode == "standard", onClick = { gameMode = "standard" }, label = { Text("Standard") })
            FilterChip(selected = gameMode == "timebank", onClick = { gameMode = "timebank" }, label = { Text("Time Bank") })
            if (!isTeamMode) {
                FilterChip(selected = gameMode == "survival", onClick = { gameMode = "survival" }, label = { Text("Survival") })
            } else if (gameMode == "survival") {
                gameMode = "standard"
            }
        }

        when (gameMode) {
            "standard" -> {
                Text("Rounds", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 7, 10).forEach { r ->
                        FilterChip(selected = rounds == r, onClick = { rounds = r }, label = { Text("$r") })
                    }
                }
            }
            "timebank" -> {
                Text("Starting bank", style = MaterialTheme.typography.bodyMedium)
                if (isTeamMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2.0, 3.0, 4.0, 5.0, 6.0).forEach { option ->
                            FilterChip(selected = bankSeconds == option, onClick = { bankSeconds = option }, label = { Text("${option.toInt()}s") })
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1.0, 2.0, 3.0, 4.0).forEach { option ->
                            FilterChip(selected = bankSeconds == option, onClick = { bankSeconds = option }, label = { Text("${option.toInt()}s") })
                        }
                    }
                }
                Text(
                    text = if (isTeamMode) "The worst miss each round drains the whole team's bank. Last team with bank remaining wins!"
                           else "Miss the target and lose that time from your bank. Last one with bank remaining wins!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            "survival" -> Text(
                text = "Go over the target time or be the farthest away — you're eliminated. Last player standing wins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }

        if (isTeamMode && !teamsValid) {
            Text(
                text = "Each team needs at least one player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val pairs = names.mapIndexed { i, n ->
                    n.ifBlank { "Player ${i + 1}" } to (if (isTeamMode) teamIds.getOrElse(i) { 1 } else 0)
                }
                onStart(pairs, GameConfig(winTarget = rounds, gameMode = gameMode, timeBankSeconds = bankSeconds, isTeamMode = isTeamMode, teamCount = teamCount))
            },
            enabled = teamsValid,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start") }

        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun LocalMultiTargetReveal(
    round: Int,
    targetTime: Double,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Round $round",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text("Target", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "%.2fs".format(targetTime),
                style = MaterialTheme.typography.displayLarge,
                color = SandAmber
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Get ready...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private val zoneColors = listOf(
    Color(0xFF0A1628),
    Color(0xFF0A1E0A),
    Color(0xFF1A0A1E),
    Color(0xFF1E140A)
)

@Composable
private fun LocalMultiTapping(
    players: List<Player>,
    playerStartNanos: Map<String, Long>,
    tapTimes: Map<String, Double>,
    targetTime: Double,
    onTap: (String) -> Unit
) {
    // Only show zones for players still alive
    val active = players.filter { !it.eliminated }

    @Composable fun zone(i: Int, rotated: Boolean, mod: Modifier) {
        val p = active[i]
        TapZone(p, playerStartNanos.containsKey(p.id), tapTimes[p.id], targetTime, zoneColors[i % zoneColors.size], rotated, mod) { onTap(p.id) }
    }

    when (active.size) {
        1 -> zone(0, false, Modifier.fillMaxSize())
        2 -> Column(modifier = Modifier.fillMaxSize()) {
            zone(0, true,  Modifier.fillMaxWidth().weight(1f))
            HorizontalDivider(thickness = 2.dp, color = Color.White.copy(alpha = 0.25f))
            zone(1, false, Modifier.fillMaxWidth().weight(1f))
        }
        3 -> Column(modifier = Modifier.fillMaxSize()) {
            zone(0, true, Modifier.fillMaxWidth().weight(1f))
            HorizontalDivider(thickness = 2.dp, color = Color.White.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                zone(1, false, Modifier.weight(1f).fillMaxHeight())
                VerticalDivider(thickness = 2.dp, color = Color.White.copy(alpha = 0.25f))
                zone(2, false, Modifier.weight(1f).fillMaxHeight())
            }
        }
        else -> Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                zone(0, true,  Modifier.weight(1f).fillMaxHeight())
                VerticalDivider(thickness = 2.dp, color = Color.White.copy(alpha = 0.25f))
                zone(1, true,  Modifier.weight(1f).fillMaxHeight())
            }
            HorizontalDivider(thickness = 2.dp, color = Color.White.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                zone(2, false, Modifier.weight(1f).fillMaxHeight())
                VerticalDivider(thickness = 2.dp, color = Color.White.copy(alpha = 0.25f))
                zone(3, false, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun TapZone(
    player: Player,
    started: Boolean,
    tapTime: Double?,
    targetTime: Double,
    backgroundColor: Color,
    rotated: Boolean,
    modifier: Modifier,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = if (tapTime != null) null else androidx.compose.material3.ripple(),
            ) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        val content: @Composable () -> Unit = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                when {
                    tapTime != null -> {
                        Text(
                            text = "TAP!",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = AccentGreen
                        )
                        Text(
                            text = "Stop when ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                    started -> {
                        Text(
                            text = "TAP!",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = AccentGreen
                        )
                        Text(
                            text = "Stop when ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                    else -> {
                        Text(
                            text = "TAP TO START",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = AccentGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Target: %.2fs".format(targetTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
        if (rotated) {
            Box(modifier = Modifier.rotate(180f)) { content() }
        } else {
            content()
        }
    }
}

@Composable
private fun LocalMultiRoundResult(
    players: List<Player>,
    results: List<RoundResult>,
    allResults: List<RoundResult>,
    isTimeBankMode: Boolean = false,
    isSurvivalMode: Boolean = false,
    isTeamMode: Boolean = false,
    isGameOver: Boolean = false,
    onNext: () -> Unit,
    onSeeWinner: () -> Unit = {},
    onHome: () -> Unit
) {
    val sorted = results.sortedBy { kotlin.math.abs(it.delta) }
    val minDelta = sorted.firstOrNull()?.let { kotlin.math.abs(it.delta) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Round Results", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (isTeamMode) {
            val teamIds = players.map { it.teamId }.distinct().sorted()
            val bestTeamId = teamIds.minByOrNull { tid ->
                val tr = sorted.filter { res -> players.any { it.teamId == tid && it.id == res.playerId } }
                if (tr.isEmpty()) Double.MAX_VALUE else tr.map { kotlin.math.abs(it.delta) }.average()
            }
            teamIds.forEach { teamId ->
                val teamResults = sorted.filter { res -> players.any { it.teamId == teamId && it.id == res.playerId } }
                val teamAvg = if (teamResults.isEmpty()) null else teamResults.map { kotlin.math.abs(it.delta) }.average()
                val teamEliminated = isTimeBankMode && players.filter { it.teamId == teamId }.all { it.eliminated }
                val isWinningTeam = teamId == bestTeamId
                Text(
                    text = buildString {
                        append(if (isWinningTeam) "★ " else "")
                        append("Team ${TEAM_LABELS_MULTI.getOrElse(teamId - 1) { "?" }}")
                        if (teamAvg != null) append("  —  %.2fs avg".format(teamAvg))
                        if (teamEliminated) append("  💀")
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isWinningTeam) FontWeight.Bold else FontWeight.Normal),
                    color = if (teamEliminated) AccentRed else if (isWinningTeam) AccentGreen else MaterialTheme.colorScheme.onSurface
                )
                if (isTimeBankMode) {
                    val teamBank = players.filter { it.teamId == teamId }.minOfOrNull { it.bank } ?: 0.0
                    Text(
                        text = if (teamEliminated) "Bank depleted" else "Bank: %.2fs remaining".format(teamBank),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (teamEliminated || teamBank < 0.3) AccentRed else AccentGreen
                    )
                }
                teamResults.forEach { result ->
                    val player = players.find { it.id == result.playerId }
                    val isEliminated = isTimeBankMode && player?.eliminated == true
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(player?.name ?: "", style = MaterialTheme.typography.bodyLarge)
                            if (isEliminated) Text("💀 ELIMINATED", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = AccentRed)
                        }
                        Text(
                            text = "%.2fs  (%+.2fs)".format(result.playerTime, result.delta),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (kotlin.math.abs(result.delta) > 1.0) AccentRed else AccentGreen
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        } else {
            sorted.forEach { result ->
                val player = players.find { it.id == result.playerId }
                val absDelta = kotlin.math.abs(result.delta)
                val isWinner = minDelta != null && absDelta == minDelta
                val playerAllResults = allResults.filter { it.playerId == result.playerId }
                val avg = if (isSurvivalMode || playerAllResults.isEmpty()) null
                          else playerAllResults.map { kotlin.math.abs(it.delta) }.average()
                val isEliminated = (isSurvivalMode || isTimeBankMode) && player?.eliminated == true
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isWinner) "★ ${player?.name ?: ""}" else (player?.name ?: ""),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal),
                            color = if (isWinner) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                        if (isEliminated) Text("💀 ELIMINATED", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = AccentRed)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.2fs  (%+.2fs)".format(result.playerTime, result.delta),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (kotlin.math.abs(result.delta) > 1.0) AccentRed else AccentGreen
                        )
                        if (avg != null) Text("Avg: %.2fs".format(avg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                if (isTimeBankMode && player != null && !player.eliminated) {
                    Text("Bank: %.2fs remaining".format(player.bank), style = MaterialTheme.typography.bodyMedium, color = if (player.bank < 0.3) AccentRed else AccentGreen)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = if (isGameOver) onSeeWinner else onNext,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isGameOver) "See Winner" else "Next Round") }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("End Game") }
    }
}

@Composable
private fun LocalMultiWinner(
    players: List<Player>,
    allResults: List<RoundResult>,
    isTimeBankMode: Boolean = false,
    isSurvivalMode: Boolean = false,
    isTeamMode: Boolean = false,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    data class Score(val player: Player, val avg: Double)
    data class TeamScore(val teamId: Int, val label: String, val stat: Double, val bank: Double?)

    val teamIds = players.map { it.teamId }.distinct().sorted()

    val winnerName: String
    val teamScores: List<TeamScore>

    if (isTeamMode) {
        val bestTeamId = when {
            isTimeBankMode -> teamIds.maxByOrNull { tid ->
                players.filter { it.teamId == tid && !it.eliminated }.maxOfOrNull { it.bank } ?: -1.0
            }
            else -> teamIds.minByOrNull { tid ->
                val r = allResults.filter { res -> players.any { it.teamId == tid && it.id == res.playerId } }
                if (r.isEmpty()) Double.MAX_VALUE else r.map { kotlin.math.abs(it.delta) }.average()
            }
        }
        val label = TEAM_LABELS_MULTI.getOrElse((bestTeamId ?: 1) - 1) { "?" }
        winnerName = "Team $label"
        teamScores = teamIds.map { tid ->
            val r = allResults.filter { res -> players.any { it.teamId == tid && it.id == res.playerId } }
            val stat = if (r.isEmpty()) Double.MAX_VALUE else r.map { kotlin.math.abs(it.delta) }.average()
            val bank = if (isTimeBankMode) players.filter { it.teamId == tid }.minOfOrNull { it.bank } else null
            TeamScore(tid, TEAM_LABELS_MULTI.getOrElse(tid - 1) { "?" }, stat, bank)
        }.sortedBy { it.stat }
    } else {
        teamScores = emptyList()
        val winnerPlayer = when {
            isTimeBankMode -> players.filter { !it.eliminated }.maxByOrNull { it.bank } ?: players.maxByOrNull { it.bank }
            isSurvivalMode -> players.filter { !it.eliminated }.firstOrNull() ?: players.firstOrNull()
            else -> players.minByOrNull { player ->
                val r = allResults.filter { it.playerId == player.id }
                if (r.isEmpty()) Double.MAX_VALUE else r.map { kotlin.math.abs(it.delta) }.average()
            }
        }
        winnerName = winnerPlayer?.name ?: ""
    }

    val scores = if (!isTeamMode) players.map { player ->
        val r = allResults.filter { it.playerId == player.id }
        Score(player, if (r.isEmpty()) Double.MAX_VALUE
              else if (isSurvivalMode) r.minOf { kotlin.math.abs(it.delta) }
              else r.map { kotlin.math.abs(it.delta) }.average())
    }.sortedBy { it.avg } else emptyList()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", style = MaterialTheme.typography.displayLarge)
        Text(text = "$winnerName wins!", style = MaterialTheme.typography.headlineMedium, color = SandAmber)

        Spacer(Modifier.height(24.dp))
        Text("Final Scores", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        if (isTeamMode) {
            teamScores.forEachIndexed { index, score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = if (index == 0) "★ Team ${score.label}" else "Team ${score.label}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal),
                            color = if (index == 0) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (isTimeBankMode && score.bank != null) "%.2fs bank".format(score.bank)
                               else "%.2fs avg".format(score.stat),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (index) { 0 -> AccentGreen; teamScores.lastIndex -> AccentRed; else -> MaterialTheme.colorScheme.onSurface }
                    )
                }
                if (index < teamScores.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        } else {
            scores.forEachIndexed { index, score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = if (index == 0) "★ ${score.player.name}" else score.player.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal),
                            color = if (index == 0) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (isSurvivalMode) "%.2fs best timeit".format(score.avg) else "%.2fs avg".format(score.avg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (index) { 0 -> AccentGreen; scores.lastIndex -> AccentRed; else -> MaterialTheme.colorScheme.onSurface }
                    )
                }
                if (index < scores.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) { Text("Rematch") }
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        }
    }
}

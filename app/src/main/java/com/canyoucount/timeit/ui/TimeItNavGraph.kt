package com.canyoucount.timeit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.ui.screens.GameScreen
import com.canyoucount.timeit.ui.screens.HomeScreen
import com.canyoucount.timeit.ui.screens.JoinRoomScreen
import com.canyoucount.timeit.ui.screens.LobbyScreen
import com.canyoucount.timeit.ui.screens.OnlineMenuScreen
import com.canyoucount.timeit.ui.screens.ResultsScreen
import com.canyoucount.timeit.ui.screens.LocalMultiScreen
import com.canyoucount.timeit.ui.screens.SinglePlayerScreen
import com.canyoucount.timeit.ui.screens.WaitingRoomScreen
import com.canyoucount.timeit.ui.screens.WinnerScreen
import com.canyoucount.timeit.util.PlayerNameStore
import com.canyoucount.timeit.viewmodel.GamePhase
import com.canyoucount.timeit.viewmodel.GameViewModel
import com.canyoucount.timeit.viewmodel.LocalMultiPlayerViewModel
import com.canyoucount.timeit.viewmodel.OnlineGameViewModel
import com.canyoucount.timeit.viewmodel.SinglePlayerViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val SINGLE_PLAYER = "single_player"
    const val LOBBY = "lobby"
    const val GAME_LOCAL = "game_local"
    const val RESULTS_LOCAL = "results_local"
    const val WINNER_LOCAL = "winner_local"
    const val ONLINE_MENU = "online_menu"
    const val HOST_NAME = "host_name"
    const val JOIN_ROOM = "join_room"
    const val WAITING_ROOM = "waiting_room"
    const val GAME_ONLINE = "game_online"
    const val RESULTS_ONLINE = "results_online"
    const val WINNER_ONLINE = "winner_online"
    const val LOCAL_MULTI = "local_multi"
    const val CHRONOS_LOCAL = "chronos_local"
    const val CHRONOS_ONLINE = "chronos_online"
}

@Composable
fun TimeItNavGraph(
    navController: NavHostController = rememberNavController(),
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {}
) {
    val gameViewModel: GameViewModel = viewModel()
    val onlineViewModel: OnlineGameViewModel = viewModel()
    val singlePlayerViewModel: SinglePlayerViewModel = viewModel()
    val localMultiViewModel: LocalMultiPlayerViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onSinglePlayer = {
                    singlePlayerViewModel.restart()
                    navController.navigate(Routes.SINGLE_PLAYER)
                },
                onPassAndPlay = { navController.navigate(Routes.LOBBY) },
                onLocalMultiplayer = {
                    localMultiViewModel.restart()
                    navController.navigate(Routes.LOCAL_MULTI)
                },
                onPlayOnline = { navController.navigate(Routes.ONLINE_MENU) },
                isMuted = isMuted,
                onToggleMute = onToggleMute
            )
        }

        composable(Routes.SINGLE_PLAYER) {
            SinglePlayerScreen(
                viewModel = singlePlayerViewModel,
                onHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.LOCAL_MULTI) {
            LocalMultiScreen(
                viewModel = localMultiViewModel,
                onHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.LOBBY) {
            LobbyScreen(onStartGame = { pairs, config ->
                gameViewModel.startGame(pairs, config)
                navController.navigate(Routes.GAME_LOCAL)
            })
        }

        composable(Routes.GAME_LOCAL) {
            GameScreen(
                viewModel = gameViewModel,
                onRoundFinished = {
                    if (gameViewModel.chronosPlayerName.value != null) {
                        navController.navigate(Routes.CHRONOS_LOCAL) { popUpTo(Routes.GAME_LOCAL) { inclusive = true } }
                    } else {
                        navController.navigate(Routes.RESULTS_LOCAL) { popUpTo(Routes.GAME_LOCAL) { inclusive = true } }
                    }
                }
            )
        }

        composable(Routes.RESULTS_LOCAL) {
            val state by gameViewModel.gameState.collectAsState()
            val results by gameViewModel.lastRoundResults.collectAsState()
            val isOver by gameViewModel.isGameOver.collectAsState()
            ResultsScreen(
                players = state.players,
                roundResults = results,
                allResults = state.results,
                isHost = true,
                isTimeBankMode = state.config.gameMode == "timebank",
                isSurvivalMode = state.config.gameMode == "survival",
                isTeamMode = state.config.isTeamMode,
                isGameOver = isOver,
                onNextRound = {
                    if (isOver) {
                        navController.navigate(Routes.WINNER_LOCAL) { popUpTo(Routes.RESULTS_LOCAL) { inclusive = true } }
                    } else {
                        gameViewModel.startNextRound()
                        navController.navigate(Routes.GAME_LOCAL) { popUpTo(Routes.RESULTS_LOCAL) { inclusive = true } }
                    }
                },
                onEndGame = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.WINNER_LOCAL) {
            val state by gameViewModel.gameState.collectAsState()
            val lastRoundResults by gameViewModel.lastRoundResults.collectAsState()
            val isTimeBankMode = state.config.gameMode == "timebank"
            val isSurvivalMode = state.config.gameMode == "survival"
            val isTeamMode = state.config.isTeamMode
            val teamLabels = listOf("A", "B", "C", "D")

            val winnerName: String
            val winnerAvgDelta: Double?
            val winningTeamId: Int?

            if (isTeamMode) {
                val teamIds = state.players.map { it.teamId }.distinct()
                val bestTeamId = when {
                    isTimeBankMode -> teamIds.maxByOrNull { tid ->
                        state.players.filter { it.teamId == tid && !it.eliminated }.maxOfOrNull { it.bank } ?: -1.0
                    }
                    else -> teamIds.maxByOrNull { tid ->
                        state.players.filter { it.teamId == tid }.map { it.wins }.average().takeIf { it.isFinite() } ?: -1.0
                    }
                }
                winningTeamId = bestTeamId
                winnerName = "Team ${teamLabels.getOrElse((bestTeamId ?: 1) - 1) { "?" }}"
                winnerAvgDelta = null
            } else {
                winningTeamId = null
                val winner = when {
                    isTimeBankMode || isSurvivalMode ->
                        state.players.filter { !it.eliminated }.firstOrNull() ?: state.players.firstOrNull()
                    else -> state.players.minByOrNull { player ->
                        val r = state.results.filter { it.playerId == player.id }
                        if (r.isEmpty()) Double.MAX_VALUE else r.map { kotlin.math.abs(it.delta) }.average()
                    }
                }
                winnerName = winner?.name ?: ""
                winnerAvgDelta = if (!isTimeBankMode && winner != null) {
                    val r = state.results.filter { it.playerId == winner.id }
                    if (r.isEmpty()) null else r.map { kotlin.math.abs(it.delta) }.average()
                } else null
            }

            WinnerScreen(
                winnerName = winnerName,
                winnerAvgDelta = winnerAvgDelta,
                players = state.players,
                allResults = state.results,
                roundResults = lastRoundResults,
                isTimeBankMode = isTimeBankMode,
                isSurvivalMode = isSurvivalMode,
                isTeamMode = isTeamMode,
                winningTeamId = winningTeamId,
                onRematch = {
                    gameViewModel.playAgain()
                    navController.navigate(Routes.GAME_LOCAL) { popUpTo(Routes.WINNER_LOCAL) { inclusive = true } }
                },
                onHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.CHRONOS_LOCAL) {
            val state by gameViewModel.gameState.collectAsState()
            val results by gameViewModel.lastRoundResults.collectAsState()
            val chronosName by gameViewModel.chronosPlayerName.collectAsState()
            ChronosScreen(
                chronosPlayerName = chronosName ?: "",
                roundResults = results,
                players = state.players,
                onSeeWinner = {
                    navController.navigate(Routes.WINNER_LOCAL) { popUpTo(Routes.CHRONOS_LOCAL) { inclusive = true } }
                }
            )
        }

        composable(Routes.CHRONOS_ONLINE) {
            val players by onlineViewModel.players.collectAsState()
            val results by onlineViewModel.lastRoundResults.collectAsState()
            val chronosName by onlineViewModel.chronosPlayerName.collectAsState()
            ChronosScreen(
                chronosPlayerName = chronosName ?: "",
                roundResults = results,
                players = players,
                onSeeWinner = {
                    navController.navigate(Routes.WINNER_ONLINE) { popUpTo(Routes.CHRONOS_ONLINE) { inclusive = true } }
                }
            )
        }

        composable(Routes.ONLINE_MENU) {
            OnlineMenuScreen(
                onHostGame = { navController.navigate(Routes.HOST_NAME) },
                onJoinGame = { navController.navigate(Routes.JOIN_ROOM) }
            )
        }

        composable(Routes.HOST_NAME) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val savedName by PlayerNameStore.getName(context).collectAsState(initial = "")
            var hostName by remember(savedName) { mutableStateOf(savedName) }
            val error by onlineViewModel.errorMessage.collectAsState()
            com.canyoucount.timeit.ui.screens.HostNameScreen(
                playerName = hostName,
                onPlayerNameChange = { hostName = it },
                errorMessage = error,
                onHost = { winTarget, gameMode, timeBankSeconds, isTeamMode, teamCount ->
                    scope.launch { PlayerNameStore.saveName(context, hostName) }
                    onlineViewModel.hostGame(hostName, GameConfig(winTarget = winTarget, gameMode = gameMode, timeBankSeconds = timeBankSeconds, isTeamMode = isTeamMode, teamCount = teamCount))
                    navController.navigate(Routes.WAITING_ROOM) { popUpTo(Routes.ONLINE_MENU) }
                }
            )
        }

        composable(Routes.JOIN_ROOM) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val savedName by PlayerNameStore.getName(context).collectAsState(initial = "")
            var playerName by remember(savedName) { mutableStateOf(savedName) }
            val error by onlineViewModel.errorMessage.collectAsState()
            JoinRoomScreen(
                playerName = playerName,
                onPlayerNameChange = { playerName = it },
                isJoining = false,
                errorMessage = error,
                onJoinRoom = { code ->
                    scope.launch { PlayerNameStore.saveName(context, playerName) }
                    onlineViewModel.joinGame(playerName, code)
                    navController.navigate(Routes.WAITING_ROOM) { popUpTo(Routes.ONLINE_MENU) }
                }
            )
        }

        composable(Routes.WAITING_ROOM) {
            val code by onlineViewModel.roomCode.collectAsState()
            val players by onlineViewModel.players.collectAsState()
            val isHost by onlineViewModel.isHost.collectAsState()
            val phase by onlineViewModel.phase.collectAsState()
            LaunchedEffect(phase) {
                if (phase != GamePhase.Lobby) {
                    navController.navigate(Routes.GAME_ONLINE) { popUpTo(Routes.WAITING_ROOM) { inclusive = true } }
                }
            }
            val onlineConfig = onlineViewModel.gameConfig
            WaitingRoomScreen(
                roomCode = code,
                players = players,
                isHost = isHost,
                isTeamMode = onlineConfig.isTeamMode,
                teamCount = onlineConfig.teamCount,
                onAssignTeam = { playerId, teamId -> onlineViewModel.updatePlayerTeam(playerId, teamId) },
                onStartGame = { onlineViewModel.startRound() }
            )
        }

        composable(Routes.GAME_ONLINE) {
            val phase by onlineViewModel.phase.collectAsState()
            val targetTime by onlineViewModel.targetTime.collectAsState()
            com.canyoucount.timeit.ui.screens.OnlineGameScreen(
                phase = phase,
                targetTime = targetTime,
                onTargetRevealFinished = onlineViewModel::onTargetRevealFinished,
                onCountdownGo = onlineViewModel::onCountdownGo,
                onCountdownFinished = onlineViewModel::onCountdownFinished,
                onTap = onlineViewModel::onPlayerTap
            )
            LaunchedEffect(phase) {
                when (phase) {
                    GamePhase.Chronos -> navController.navigate(Routes.CHRONOS_ONLINE) { popUpTo(Routes.GAME_ONLINE) { inclusive = true } }
                    GamePhase.Results, GamePhase.Winner -> navController.navigate(Routes.RESULTS_ONLINE) { popUpTo(Routes.GAME_ONLINE) { inclusive = true } }
                    else -> {}
                }
            }
        }

        composable(Routes.RESULTS_ONLINE) {
            val players by onlineViewModel.players.collectAsState()
            val results by onlineViewModel.lastRoundResults.collectAsState()
            val isHost by onlineViewModel.isHost.collectAsState()
            val phase by onlineViewModel.phase.collectAsState()
            val isGameOver by onlineViewModel.isGameOver.collectAsState()
            val allReady = players.isNotEmpty() && players.all { it.ready }
            LaunchedEffect(phase) {
                if (phase == GamePhase.TargetReveal) {
                    navController.navigate(Routes.GAME_ONLINE) { popUpTo(Routes.RESULTS_ONLINE) { inclusive = true } }
                }
            }
            LaunchedEffect(allReady) {
                if (allReady && isHost) {
                    if (isGameOver) {
                        navController.navigate(Routes.WINNER_ONLINE) { popUpTo(Routes.RESULTS_ONLINE) { inclusive = true } }
                    } else {
                        onlineViewModel.nextRound()
                    }
                }
            }
            val gameMode by onlineViewModel.gameMode.collectAsState()
            ResultsScreen(
                players = players,
                roundResults = results,
                isHost = isHost,
                isTimeBankMode = gameMode == "timebank",
                isSurvivalMode = gameMode == "survival",
                isTeamMode = onlineViewModel.gameConfig.isTeamMode,
                isGameOver = isGameOver,
                onReady = onlineViewModel::markReady,
                onNextRound = {
                    if (isGameOver) {
                        navController.navigate(Routes.WINNER_ONLINE) { popUpTo(Routes.RESULTS_ONLINE) { inclusive = true } }
                    } else {
                        onlineViewModel.nextRound()
                    }
                },
                onEndGame = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.WINNER_ONLINE) {
            val players by onlineViewModel.players.collectAsState()
            val gameMode by onlineViewModel.gameMode.collectAsState()
            val lastRoundResults by onlineViewModel.lastRoundResults.collectAsState()
            val isHost by onlineViewModel.isHost.collectAsState()
            val phase by onlineViewModel.phase.collectAsState()
            val isTimeBankMode = gameMode == "timebank"
            val isSurvivalMode = gameMode == "survival"
            val isTeamMode = onlineViewModel.gameConfig.isTeamMode
            val teamLabels = listOf("A", "B", "C", "D")
            var localVoted by remember { mutableStateOf(false) }
            val rematchVoteCount = players.count { it.ready }
            val allReady = players.isNotEmpty() && players.all { it.ready }

            LaunchedEffect(allReady) {
                if (allReady && isHost) {
                    onlineViewModel.restartGame()
                }
            }
            LaunchedEffect(phase) {
                if (phase == GamePhase.TargetReveal) {
                    localVoted = false
                    navController.navigate(Routes.GAME_ONLINE) { popUpTo(Routes.WINNER_ONLINE) { inclusive = true } }
                }
            }

            val winnerName: String
            val winningTeamId: Int?

            if (isTeamMode) {
                val teamIds = players.map { it.teamId }.distinct()
                val bestTeamId = when {
                    isTimeBankMode -> teamIds.maxByOrNull { tid ->
                        players.filter { it.teamId == tid && !it.eliminated }.maxOfOrNull { it.bank } ?: -1.0
                    }
                    else -> teamIds.maxByOrNull { tid ->
                        players.filter { it.teamId == tid }.map { it.wins }.average().takeIf { it.isFinite() } ?: -1.0
                    }
                }
                winningTeamId = bestTeamId
                winnerName = "Team ${teamLabels.getOrElse((bestTeamId ?: 1) - 1) { "?" }}"
            } else {
                winningTeamId = null
                val winner = when {
                    isTimeBankMode -> players.filter { !it.eliminated }.maxByOrNull { it.bank } ?: players.maxByOrNull { it.bank }
                    isSurvivalMode -> players.filter { !it.eliminated }.firstOrNull() ?: players.firstOrNull()
                    else -> players.maxByOrNull { it.wins }
                }
                winnerName = winner?.name ?: ""
            }

            WinnerScreen(
                winnerName = winnerName,
                players = players,
                roundResults = lastRoundResults,
                isTimeBankMode = isTimeBankMode,
                isSurvivalMode = isSurvivalMode,
                isTeamMode = isTeamMode,
                winningTeamId = winningTeamId,
                onRematch = {
                    localVoted = true
                    onlineViewModel.markReady()
                },
                rematchVoteCount = rematchVoteCount,
                totalPlayers = players.size,
                hasVotedRematch = localVoted,
                onHome = {
                    onlineViewModel.reset()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }
    }
}

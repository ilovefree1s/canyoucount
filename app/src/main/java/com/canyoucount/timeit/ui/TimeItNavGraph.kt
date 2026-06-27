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
import com.canyoucount.timeit.ui.screens.SinglePlayerScreen
import com.canyoucount.timeit.ui.screens.WaitingRoomScreen
import com.canyoucount.timeit.ui.screens.WinnerScreen
import com.canyoucount.timeit.util.PlayerNameStore
import com.canyoucount.timeit.viewmodel.GamePhase
import com.canyoucount.timeit.viewmodel.GameViewModel
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
}

@Composable
fun TimeItNavGraph(navController: NavHostController = rememberNavController()) {
    val gameViewModel: GameViewModel = viewModel()
    val onlineViewModel: OnlineGameViewModel = viewModel()
    val singlePlayerViewModel: SinglePlayerViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onSinglePlayer = {
                    singlePlayerViewModel.restart()
                    navController.navigate(Routes.SINGLE_PLAYER)
                },
                onPassAndPlay = { navController.navigate(Routes.LOBBY) },
                onPlayOnline = { navController.navigate(Routes.ONLINE_MENU) }
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

        composable(Routes.LOBBY) {
            LobbyScreen(onStartGame = { names, config ->
                gameViewModel.startGame(names, config)
                navController.navigate(Routes.GAME_LOCAL)
            })
        }

        composable(Routes.GAME_LOCAL) {
            GameScreen(
                viewModel = gameViewModel,
                onRoundFinished = {
                    val phase = gameViewModel.phase.value
                    val destination = if (phase == GamePhase.Winner) Routes.WINNER_LOCAL else Routes.RESULTS_LOCAL
                    navController.navigate(destination) { popUpTo(Routes.GAME_LOCAL) { inclusive = true } }
                }
            )
        }

        composable(Routes.RESULTS_LOCAL) {
            val state by gameViewModel.gameState.collectAsState()
            val results by gameViewModel.lastRoundResults.collectAsState()
            ResultsScreen(
                players = state.players,
                roundResults = results,
                isHost = true,
                isTimeBankMode = state.config.gameMode == "timebank",
                onNextRound = {
                    gameViewModel.startNextRound()
                    navController.navigate(Routes.GAME_LOCAL) { popUpTo(Routes.RESULTS_LOCAL) { inclusive = true } }
                },
                onEndGame = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.WINNER_LOCAL) {
            val state by gameViewModel.gameState.collectAsState()
            val winner = if (state.config.gameMode == "timebank") {
                state.players.filter { !it.eliminated }.maxByOrNull { it.bank }
                    ?: state.players.maxByOrNull { it.bank }
            } else {
                state.players.maxByOrNull { it.wins }
            }
            WinnerScreen(
                winnerName = winner?.name ?: "",
                onPlayAgain = {
                    gameViewModel.playAgain()
                    navController.navigate(Routes.GAME_LOCAL) { popUpTo(Routes.WINNER_LOCAL) { inclusive = true } }
                },
                onHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
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
                onHost = { winTarget, gameMode, timeBankSeconds ->
                    scope.launch { PlayerNameStore.saveName(context, hostName) }
                    onlineViewModel.hostGame(hostName, GameConfig(winTarget = winTarget, gameMode = gameMode, timeBankSeconds = timeBankSeconds))
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
            WaitingRoomScreen(
                roomCode = code,
                players = players,
                isHost = isHost,
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
                if (phase == GamePhase.Results || phase == GamePhase.Winner) {
                    val destination = if (phase == GamePhase.Winner) Routes.WINNER_ONLINE else Routes.RESULTS_ONLINE
                    navController.navigate(destination) { popUpTo(Routes.GAME_ONLINE) { inclusive = true } }
                }
            }
        }

        composable(Routes.RESULTS_ONLINE) {
            val players by onlineViewModel.players.collectAsState()
            val results by onlineViewModel.lastRoundResults.collectAsState()
            val isHost by onlineViewModel.isHost.collectAsState()
            val phase by onlineViewModel.phase.collectAsState()
            val allReady = players.isNotEmpty() && players.all { it.ready }
            LaunchedEffect(phase) {
                if (phase == GamePhase.TargetReveal) {
                    navController.navigate(Routes.GAME_ONLINE) { popUpTo(Routes.RESULTS_ONLINE) { inclusive = true } }
                }
            }
            LaunchedEffect(allReady) {
                if (allReady && isHost) {
                    onlineViewModel.nextRound()
                }
            }
            val gameMode by onlineViewModel.gameMode.collectAsState()
            ResultsScreen(
                players = players,
                roundResults = results,
                isHost = isHost,
                isTimeBankMode = gameMode == "timebank",
                onReady = onlineViewModel::markReady,
                onNextRound = { onlineViewModel.nextRound() },
                onEndGame = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.WINNER_ONLINE) {
            val players by onlineViewModel.players.collectAsState()
            val winner = players.maxByOrNull { it.wins }
            WinnerScreen(
                winnerName = winner?.name ?: "",
                onPlayAgain = {
                    navController.navigate(Routes.ONLINE_MENU) { popUpTo(Routes.HOME) }
                },
                onHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }
    }
}

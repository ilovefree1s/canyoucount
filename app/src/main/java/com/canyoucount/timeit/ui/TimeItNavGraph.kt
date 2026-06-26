package com.canyoucount.timeit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.canyoucount.timeit.ui.screens.GameScreen
import com.canyoucount.timeit.ui.screens.HomeScreen
import com.canyoucount.timeit.ui.screens.JoinRoomScreen
import com.canyoucount.timeit.ui.screens.LobbyScreen
import com.canyoucount.timeit.ui.screens.OnlineMenuScreen
import com.canyoucount.timeit.ui.screens.ResultsScreen
import com.canyoucount.timeit.ui.screens.WaitingRoomScreen
import com.canyoucount.timeit.ui.screens.WinnerScreen
import com.canyoucount.timeit.viewmodel.GamePhase
import com.canyoucount.timeit.viewmodel.GameViewModel
import com.canyoucount.timeit.viewmodel.OnlineGameViewModel

private object Routes {
    const val HOME = "home"
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

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onPassAndPlay = { navController.navigate(Routes.LOBBY) },
                onPlayOnline = { navController.navigate(Routes.ONLINE_MENU) }
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
            val winner = state.players.maxByOrNull { it.wins }
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
            var hostName by remember { mutableStateOf("") }
            val error by onlineViewModel.errorMessage.collectAsState()
            com.canyoucount.timeit.ui.screens.HostNameScreen(
                playerName = hostName,
                onPlayerNameChange = { hostName = it },
                errorMessage = error,
                onHost = {
                    onlineViewModel.hostGame(hostName)
                    navController.navigate(Routes.WAITING_ROOM) { popUpTo(Routes.ONLINE_MENU) }
                }
            )
        }

        composable(Routes.JOIN_ROOM) {
            var playerName by remember { mutableStateOf("") }
            val error by onlineViewModel.errorMessage.collectAsState()
            JoinRoomScreen(
                playerName = playerName,
                onPlayerNameChange = { playerName = it },
                isJoining = false,
                errorMessage = error,
                onJoinRoom = { code ->
                    onlineViewModel.joinGame(playerName, code)
                    navController.navigate(Routes.WAITING_ROOM) { popUpTo(Routes.ONLINE_MENU) }
                }
            )
        }

        composable(Routes.WAITING_ROOM) {
            val code by onlineViewModel.roomCode.collectAsState()
            val players by onlineViewModel.players.collectAsState()
            val isHost by onlineViewModel.isHost.collectAsState()
            WaitingRoomScreen(
                roomCode = code,
                players = players,
                isHost = isHost,
                onStartGame = {
                    onlineViewModel.startRound()
                    navController.navigate(Routes.GAME_ONLINE) { popUpTo(Routes.WAITING_ROOM) { inclusive = true } }
                }
            )
        }

        composable(Routes.GAME_ONLINE) {
            val phase by onlineViewModel.phase.collectAsState()
            val targetTime by onlineViewModel.targetTime.collectAsState()
            com.canyoucount.timeit.ui.screens.OnlineGameScreen(
                phase = phase,
                targetTime = targetTime,
                onTap = onlineViewModel::onPlayerTap
            )
            if (phase == GamePhase.Results || phase == GamePhase.Winner) {
                val destination = if (phase == GamePhase.Winner) Routes.WINNER_ONLINE else Routes.RESULTS_ONLINE
                navController.navigate(destination) { popUpTo(Routes.GAME_ONLINE) { inclusive = true } }
            }
        }

        composable(Routes.RESULTS_ONLINE) {
            val players by onlineViewModel.players.collectAsState()
            val results by onlineViewModel.lastRoundResults.collectAsState()
            val isHost by onlineViewModel.isHost.collectAsState()
            ResultsScreen(
                players = players,
                roundResults = results,
                isHost = isHost,
                onNextRound = {
                    onlineViewModel.nextRound()
                    navController.navigate(Routes.GAME_ONLINE) { popUpTo(Routes.RESULTS_ONLINE) { inclusive = true } }
                },
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

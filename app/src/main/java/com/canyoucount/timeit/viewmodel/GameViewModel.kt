package com.canyoucount.timeit.viewmodel

import androidx.lifecycle.ViewModel
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.GameState
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.util.RoomCodeUtil
import com.canyoucount.timeit.util.TimerUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * Drives the pass-and-play local game: sequential turns on a single device.
 */
class GameViewModel : ViewModel() {

    private val _phase = MutableStateFlow(GamePhase.TargetReveal)
    val phase: StateFlow<GamePhase> = _phase

    private val _gameState = MutableStateFlow(
        GameState(
            config = GameConfig(),
            players = emptyList(),
            currentRound = 1,
            results = emptyList()
        )
    )
    val gameState: StateFlow<GameState> = _gameState

    private val _targetTime = MutableStateFlow(0.0)
    val targetTime: StateFlow<Double> = _targetTime

    private val _currentPlayerIndex = MutableStateFlow(0)
    val currentPlayerIndex: StateFlow<Int> = _currentPlayerIndex

    private val _lastRoundResults = MutableStateFlow<List<RoundResult>>(emptyList())
    val lastRoundResults: StateFlow<List<RoundResult>> = _lastRoundResults

    private var tapStartNanos: Long = 0L
    private val pendingTimes = mutableMapOf<String, Double>()

    fun startGame(playerNames: List<String>, config: GameConfig = GameConfig()) {
        val players = playerNames.map {
            Player(id = RoomCodeUtil.generate(), name = it, bank = config.timeBankSeconds)
        }
        _gameState.value = GameState(
            config = config,
            players = players,
            currentRound = 1,
            results = emptyList()
        )
        _currentPlayerIndex.value = 0
        pendingTimes.clear()
        rollTargetTime(config)
        _phase.value = GamePhase.TargetReveal
    }

    private fun rollTargetTime(config: GameConfig = _gameState.value.config) {
        _targetTime.value = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
    }

    fun onTargetRevealFinished() {
        _phase.value = GamePhase.Countdown
    }

    fun onCountdownGo() {
        tapStartNanos = TimerUtil.now()
    }

    fun onCountdownFinished() {
        _phase.value = GamePhase.Tapping
    }

    /** Called when the current player taps the screen during the Tapping phase. */
    fun onPlayerTap() {
        val players = _gameState.value.players
        val currentPlayer = players[_currentPlayerIndex.value]
        val elapsed = TimerUtil.round2(TimerUtil.elapsedSeconds(tapStartNanos))
        pendingTimes[currentPlayer.id] = elapsed

        val nextIndex = nextActivePlayers(players, _currentPlayerIndex.value + 1)
        if (nextIndex != -1) {
            _currentPlayerIndex.value = nextIndex
            _phase.value = GamePhase.TargetReveal
        } else {
            finishRound()
        }
    }

    private fun nextActivePlayers(players: List<Player>, fromIndex: Int): Int {
        for (i in fromIndex until players.size) {
            if (!players[i].eliminated) return i
        }
        return -1
    }

    private fun finishRound() {
        val target = _targetTime.value
        val config = _gameState.value.config
        val roundResults = pendingTimes.map { (playerId, time) ->
            RoundResult(
                playerId = playerId,
                targetTime = target,
                playerTime = time,
                delta = TimerUtil.round2(time - target)
            )
        }
        _lastRoundResults.value = roundResults

        val updatedPlayers = if (config.gameMode == "timebank") {
            _gameState.value.players.map { player ->
                val result = roundResults.find { it.playerId == player.id }
                if (result != null) {
                    val newBank = TimerUtil.round2(player.bank - kotlin.math.abs(result.delta))
                    player.copy(bank = newBank, eliminated = newBank <= 0.0)
                } else player
            }
        } else {
            val minAbsDelta = roundResults.minOf { kotlin.math.abs(it.delta) }
            val roundWinnerIds = roundResults
                .filter { kotlin.math.abs(it.delta) == minAbsDelta }
                .map { it.playerId }
            _gameState.value.players.map { player ->
                if (player.id in roundWinnerIds) player.copy(wins = player.wins + 1) else player
            }
        }

        _gameState.update {
            it.copy(players = updatedPlayers, results = it.results + roundResults)
        }

        _phase.value = when {
            config.gameMode == "timebank" && updatedPlayers.count { !it.eliminated } <= 1 -> GamePhase.Winner
            config.gameMode == "standard" && updatedPlayers.any { it.wins >= config.winTarget } -> GamePhase.Winner
            else -> GamePhase.Results
        }
    }

    fun startNextRound() {
        pendingTimes.clear()
        val firstActive = nextActivePlayers(_gameState.value.players, 0)
        _currentPlayerIndex.value = if (firstActive != -1) firstActive else 0
        _gameState.update { it.copy(currentRound = it.currentRound + 1) }
        rollTargetTime()
        _phase.value = GamePhase.TargetReveal
    }

    fun playAgain() {
        val config = _gameState.value.config
        val resetPlayers = _gameState.value.players.map {
            it.copy(wins = 0, bank = config.timeBankSeconds, eliminated = false)
        }
        _gameState.update {
            it.copy(players = resetPlayers, currentRound = 1, results = emptyList())
        }
        pendingTimes.clear()
        _currentPlayerIndex.value = 0
        rollTargetTime()
        _phase.value = GamePhase.TargetReveal
    }
}

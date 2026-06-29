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

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver

    private val _chronosPlayerName = MutableStateFlow<String?>(null)
    val chronosPlayerName: StateFlow<String?> = _chronosPlayerName

    private var tapStartNanos: Long = 0L
    private val pendingTimes = mutableMapOf<String, Double>()

    fun startGame(playerNames: List<Pair<String, Int>>, config: GameConfig = GameConfig()) {
        val players = playerNames.map { (name, teamId) ->
            Player(id = RoomCodeUtil.generate(), name = name, bank = config.timeBankSeconds, teamId = teamId)
        }
        _gameState.value = GameState(
            config = config,
            players = players,
            currentRound = 1,
            results = emptyList()
        )
        _currentPlayerIndex.value = 0
        _isGameOver.value = false
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

        // Chronos: any active player hits exactly the target
        val perfectResult = roundResults.firstOrNull { it.delta == 0.0 }
        if (perfectResult != null) {
            val chronosPlayer = _gameState.value.players.find { it.id == perfectResult.playerId }
            if (chronosPlayer != null) {
                _chronosPlayerName.value = chronosPlayer.name
                _isGameOver.value = true
                _gameState.update { it.copy(players = it.players, results = it.results + roundResults) }
                _phase.value = GamePhase.Chronos
                return
            }
        }

        val updatedPlayers = when (config.gameMode) {
            "timebank" -> {
                if (config.isTeamMode) {
                    // Drain the worst miss in the team from all team members equally
                    val teamPenalty = mutableMapOf<Int, Double>()
                    _gameState.value.players.forEach { player ->
                        val result = roundResults.find { it.playerId == player.id }
                        if (result != null) {
                            val penalty = kotlin.math.abs(result.delta)
                            teamPenalty[player.teamId] = maxOf(teamPenalty[player.teamId] ?: 0.0, penalty)
                        }
                    }
                    _gameState.value.players.map { player ->
                        val penalty = teamPenalty[player.teamId] ?: 0.0
                        val newBank = TimerUtil.round2(player.bank - penalty)
                        player.copy(bank = newBank, eliminated = newBank <= 0.0)
                    }
                } else {
                    _gameState.value.players.map { player ->
                        val result = roundResults.find { it.playerId == player.id }
                        if (result != null) {
                            val newBank = TimerUtil.round2(player.bank - kotlin.math.abs(result.delta))
                            player.copy(bank = newBank, eliminated = newBank <= 0.0)
                        } else player
                    }
                }
            }
            "survival" -> survivalEliminate(_gameState.value.players, roundResults)
            else -> _gameState.value.players
        }

        val allResults = _gameState.value.results + roundResults
        val currentRound = _gameState.value.currentRound

        _gameState.update { it.copy(players = updatedPlayers, results = allResults) }

        val gameOver = when (config.gameMode) {
            "timebank" -> if (config.isTeamMode) teamsAlive(updatedPlayers) <= 1
                          else updatedPlayers.count { !it.eliminated } <= 1
            "survival" -> updatedPlayers.count { !it.eliminated } <= 1
            else -> currentRound >= config.winTarget
        }
        _isGameOver.value = gameOver
        _phase.value = GamePhase.Results
    }

    private fun teamsAlive(players: List<Player>): Int =
        players.filter { !it.eliminated }.map { it.teamId }.distinct().size

    private fun survivalEliminate(players: List<Player>, results: List<RoundResult>): List<Player> {
        val active = players.filter { !it.eliminated }
        val activeResults = results.filter { r -> active.any { it.id == r.playerId } }
        if (activeResults.isEmpty()) return players

        val perfectId = activeResults.firstOrNull { it.delta == 0.0 }?.playerId
        if (perfectId != null) {
            return players.map { if (it.id != perfectId) it.copy(eliminated = true) else it }
        }

        val wentOverIds = activeResults.filter { it.delta > 0 }.map { it.playerId }.toSet()

        val eliminateIds: Set<String> = when {
            activeResults.all { it.delta > 0 } -> {
                // Everyone went over — keep only the closest (least over)
                val keepId = activeResults.minByOrNull { it.delta }?.playerId
                activeResults.map { it.playerId }.toSet() - setOfNotNull(keepId)
            }
            wentOverIds.isNotEmpty() -> wentOverIds // Some went over — only they are eliminated
            else -> {
                // Nobody went over — eliminate the farthest away (if 2+ players)
                val farthestId = if (activeResults.size > 1)
                    activeResults.maxByOrNull { kotlin.math.abs(it.delta) }?.playerId
                else null
                setOfNotNull(farthestId)
            }
        }

        return players.map { if (it.id in eliminateIds) it.copy(eliminated = true) else it }
    }

    fun startNextRound() {
        pendingTimes.clear()
        _isGameOver.value = false
        _chronosPlayerName.value = null
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

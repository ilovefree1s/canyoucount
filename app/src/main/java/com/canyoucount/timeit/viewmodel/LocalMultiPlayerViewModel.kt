package com.canyoucount.timeit.viewmodel

import androidx.lifecycle.ViewModel
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.util.RoomCodeUtil
import com.canyoucount.timeit.util.TimerUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class LocalMultiPlayerViewModel : ViewModel() {

    private val _phase = MutableStateFlow<GamePhase>(GamePhase.Setup)
    val phase: StateFlow<GamePhase> = _phase

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _targetTime = MutableStateFlow(0.0)
    val targetTime: StateFlow<Double> = _targetTime

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound

    // Per-player start nanos (set on first tap — "tap to start")
    private val _playerStartNanos = MutableStateFlow<Map<String, Long>>(emptyMap())
    val playerStartNanos: StateFlow<Map<String, Long>> = _playerStartNanos

    // Per-player elapsed time (set on second tap — "tap to stop")
    private val _tapTimes = MutableStateFlow<Map<String, Double>>(emptyMap())
    val tapTimes: StateFlow<Map<String, Double>> = _tapTimes

    private val _lastRoundResults = MutableStateFlow<List<RoundResult>>(emptyList())
    val lastRoundResults: StateFlow<List<RoundResult>> = _lastRoundResults

    private val _allResults = MutableStateFlow<List<RoundResult>>(emptyList())
    val allResults: StateFlow<List<RoundResult>> = _allResults

    private val _gameMode = MutableStateFlow("standard")
    val gameMode: StateFlow<String> = _gameMode

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver

    private val _chronosPlayerName = MutableStateFlow<String?>(null)
    val chronosPlayerName: StateFlow<String?> = _chronosPlayerName

    private var config = GameConfig()

    fun startGame(playerNames: List<Pair<String, Int>>, gameConfig: GameConfig) {
        config = gameConfig
        _gameMode.value = gameConfig.gameMode
        _players.value = playerNames.map { (name, teamId) ->
            Player(id = RoomCodeUtil.generate(), name = name, bank = gameConfig.timeBankSeconds, teamId = teamId)
        }
        _currentRound.value = 1
        _allResults.value = emptyList()
        resetRoundState()
        rollTarget()
        _phase.value = GamePhase.TargetReveal
    }

    fun onTargetRevealFinished() {
        resetRoundState()
        _phase.value = GamePhase.Tapping
    }

    fun onPlayerTap(playerId: String) {
        if (_tapTimes.value.containsKey(playerId)) return // already finished

        val startNanos = _playerStartNanos.value[playerId]
        if (startNanos == null) {
            // First tap: start this player's timer
            _playerStartNanos.update { it + (playerId to TimerUtil.now()) }
        } else {
            // Second tap: stop and record
            val elapsed = TimerUtil.round2(TimerUtil.elapsedSeconds(startNanos))
            val activePlayers = _players.value.filter { !it.eliminated }
            var updated: Map<String, Double> = emptyMap()
            _tapTimes.update { current ->
                (current + (playerId to elapsed)).also { updated = it }
            }
            if (updated.size >= activePlayers.size) finishRound()
        }
    }

    private fun finishRound() {
        val target = _targetTime.value
        val results = _tapTimes.value.map { (playerId, time) ->
            RoundResult(
                playerId = playerId,
                targetTime = target,
                playerTime = time,
                delta = TimerUtil.round2(time - target)
            )
        }
        _lastRoundResults.value = results
        _allResults.value = _allResults.value + results

        val perfectResult = results.firstOrNull { it.delta == 0.0 }
        if (perfectResult != null) {
            val chronosPlayer = _players.value.find { it.id == perfectResult.playerId }
            if (chronosPlayer != null) {
                _chronosPlayerName.value = chronosPlayer.name
                _isGameOver.value = true
                _phase.value = GamePhase.Chronos
                return
            }
        }

        val updatedPlayers = when (config.gameMode) {
            "timebank" -> {
                if (config.isTeamMode) {
                    val teamPenalty = mutableMapOf<Int, Double>()
                    _players.value.forEach { player ->
                        val result = results.find { it.playerId == player.id }
                        if (result != null) {
                            val penalty = kotlin.math.abs(result.delta)
                            teamPenalty[player.teamId] = maxOf(teamPenalty[player.teamId] ?: 0.0, penalty)
                        }
                    }
                    _players.value.map { player ->
                        val penalty = teamPenalty[player.teamId] ?: 0.0
                        val newBank = TimerUtil.round2(player.bank - penalty)
                        player.copy(bank = newBank, eliminated = newBank <= 0.0)
                    }
                } else {
                    _players.value.map { player ->
                        val result = results.find { it.playerId == player.id }
                        if (result != null) {
                            val newBank = TimerUtil.round2(player.bank - kotlin.math.abs(result.delta))
                            player.copy(bank = newBank, eliminated = newBank <= 0.0)
                        } else player
                    }
                }
            }
            "survival" -> survivalEliminate(_players.value, results)
            else -> _players.value
        }
        _players.value = updatedPlayers

        val gameOver = when (config.gameMode) {
            "timebank" -> if (config.isTeamMode) teamsAlive(updatedPlayers) <= 1
                          else updatedPlayers.count { !it.eliminated } <= 1
            "survival" -> updatedPlayers.count { !it.eliminated } <= 1
            else -> _currentRound.value >= config.winTarget
        }
        _isGameOver.value = gameOver

        // Skip results screen if game ends on round 1 (timebank or survival)
        val skipResults = gameOver && _currentRound.value == 1 &&
                (config.gameMode == "timebank" || config.gameMode == "survival")
        _phase.value = if (skipResults) GamePhase.Winner else GamePhase.RoundResult
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
                val keepId = activeResults.minByOrNull { it.delta }?.playerId
                activeResults.map { it.playerId }.toSet() - setOfNotNull(keepId)
            }
            wentOverIds.isNotEmpty() -> wentOverIds
            else -> {
                val farthestId = if (activeResults.size > 1)
                    activeResults.maxByOrNull { kotlin.math.abs(it.delta) }?.playerId
                else null
                setOfNotNull(farthestId)
            }
        }

        return players.map { if (it.id in eliminateIds) it.copy(eliminated = true) else it }
    }

    fun nextRound() {
        _currentRound.value++
        _isGameOver.value = false
        _chronosPlayerName.value = null
        resetRoundState()
        rollTarget()
        _phase.value = GamePhase.TargetReveal
    }

    fun goToWinner() {
        _phase.value = GamePhase.Winner
    }

    fun restart() {
        _phase.value = GamePhase.Setup
    }

    private fun resetRoundState() {
        _playerStartNanos.value = emptyMap()
        _tapTimes.value = emptyMap()
    }

    private fun rollTarget() {
        _targetTime.value = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
    }
}

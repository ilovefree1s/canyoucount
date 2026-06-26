package com.canyoucount.timeit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.data.repository.SupabaseRepository
import com.canyoucount.timeit.util.TimerUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.UUID

/**
 * Drives online multiplayer via Supabase: room creation/joining, the host-broadcast
 * "GO" signal (a `rooms.status -> playing` row update observed over Realtime), and
 * result submission/collection. Each device times itself from the moment it observes
 * the GO signal locally — raw clocks are never compared across devices (see spec 3.2).
 */
class OnlineGameViewModel(
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {

    private val localHostId: String = UUID.randomUUID().toString()
    private var localRoomPlayerId: String? = null

    private val _phase = MutableStateFlow(GamePhase.Waiting)
    val phase: StateFlow<GamePhase> = _phase

    private val _roomId = MutableStateFlow<String?>(null)
    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _targetTime = MutableStateFlow(0.0)
    val targetTime: StateFlow<Double> = _targetTime

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound

    private val _lastRoundResults = MutableStateFlow<List<RoundResult>>(emptyList())
    val lastRoundResults: StateFlow<List<RoundResult>> = _lastRoundResults

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var config: GameConfig = GameConfig()
    private var tapStartNanos: Long = 0L

    fun hostGame(playerName: String, gameConfig: GameConfig = GameConfig()) {
        config = gameConfig
        viewModelScope.launch {
            try {
                repository.connectRealtime()
                val room = repository.createRoom(hostId = localHostId, config = gameConfig)
                val hostPlayer = repository.joinRoom(room.id!!, playerName)
                localRoomPlayerId = hostPlayer.id
                _roomId.value = room.id
                _roomCode.value = room.code
                _isHost.value = true
                _players.value = listOf(Player(id = hostPlayer.id!!, name = playerName))
                observeRoom()
                observePlayers()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to host game: ${e.message}"
            }
        }
    }

    fun joinGame(playerName: String, code: String) {
        viewModelScope.launch {
            try {
                repository.connectRealtime()
                val room = repository.findRoomByCode(code)
                if (room == null) {
                    _errorMessage.value = "No room found with code $code"
                    return@launch
                }
                val joined = repository.joinRoom(room.id!!, playerName)
                localRoomPlayerId = joined.id
                _roomId.value = room.id
                _roomCode.value = room.code
                _isHost.value = false
                config = room.config
                refreshPlayers()
                observeRoom()
                observePlayers()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to join room: ${e.message}"
            }
        }
    }

    private suspend fun refreshPlayers() {
        val roomId = _roomId.value ?: return
        _players.value = repository.listPlayers(roomId).map {
            Player(id = it.id ?: "", name = it.player_name, wins = it.wins)
        }
    }

    private fun observePlayers() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            repository.observePlayers(roomId).collect {
                refreshPlayers()
            }
        }
    }

    private fun observeRoom() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            repository.observeRoom(roomId).collect {
                val room = repository.findRoomByCode(_roomCode.value) ?: return@collect
                if (room.status == "playing" && room.target_time != null) {
                    _targetTime.value = room.target_time
                    _currentRound.value = room.current_round
                    onGoSignalReceived()
                }
            }
        }
    }

    /** Host-only: rolls a new target and flips the room to "playing", which all
     * clients observe as the GO signal via Realtime. */
    fun startRound() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            val target = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
            repository.startRound(roomId, target, _currentRound.value)
        }
    }

    private fun onGoSignalReceived() {
        tapStartNanos = TimerUtil.now()
        _phase.value = GamePhase.Tapping
    }

    fun onPlayerTap() {
        val roomId = _roomId.value ?: return
        val playerId = localRoomPlayerId ?: return
        val elapsed = TimerUtil.round2(TimerUtil.elapsedSeconds(tapStartNanos))
        _phase.value = GamePhase.Waiting
        viewModelScope.launch {
            repository.submitResult(
                com.canyoucount.timeit.data.model.RoundResultRow(
                    room_id = roomId,
                    round = _currentRound.value,
                    player_id = playerId,
                    player_time = elapsed,
                    delta = TimerUtil.round2(elapsed - _targetTime.value)
                )
            )
            awaitAllResultsOrTimeout()
        }
    }

    private suspend fun awaitAllResultsOrTimeout() {
        val roomId = _roomId.value ?: return
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val results = repository.listResults(roomId, _currentRound.value)
            if (results.size >= _players.value.size) break
            delay(500)
        }
        finishRound()
    }

    private suspend fun finishRound() {
        val roomId = _roomId.value ?: return
        val rows = repository.listResults(roomId, _currentRound.value)
        val target = _targetTime.value
        val results = rows.map {
            RoundResult(
                playerId = it.player_id,
                targetTime = target,
                playerTime = it.player_time,
                delta = it.delta
            )
        }
        _lastRoundResults.value = results

        val minAbsDelta = results.minOfOrNull { kotlin.math.abs(it.delta) }
        val roundWinnerIds = results.filter { kotlin.math.abs(it.delta) == minAbsDelta }.map { it.playerId }
        if (_isHost.value) {
            repository.incrementWins(roundWinnerIds)
        }
        refreshPlayers()

        _phase.value = if (_players.value.any { it.wins >= config.winTarget }) {
            GamePhase.Winner
        } else {
            GamePhase.Results
        }
    }

    fun nextRound() {
        _currentRound.value += 1
        startRound()
    }
}

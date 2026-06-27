package com.canyoucount.timeit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.Player
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.data.repository.SupabaseRepository
import com.canyoucount.timeit.util.TimerUtil
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private val _phase = MutableStateFlow(GamePhase.Lobby)
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

    private val _gameMode = MutableStateFlow("standard")
    val gameMode: StateFlow<String> = _gameMode
    private val activeChannels = mutableListOf<RealtimeChannel>()

    fun hostGame(playerName: String, gameConfig: GameConfig = GameConfig()) {
        config = gameConfig
        _gameMode.value = gameConfig.gameMode
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
                _gameMode.value = room.config.gameMode
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
        val existing = _players.value.associateBy { it.id }
        _players.value = repository.listPlayers(roomId).map {
            val prev = existing[it.id ?: ""]
            Player(
                id = it.id ?: "",
                name = it.player_name,
                wins = it.wins,
                ready = it.ready,
                bank = prev?.bank ?: 1.0,
                eliminated = prev?.eliminated ?: false
            )
        }
    }

    fun markReady() {
        val playerId = localRoomPlayerId ?: return
        viewModelScope.launch {
            runCatching { repository.markReady(playerId) }
        }
    }

    private fun observePlayers() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val (channel, flow) = repository.observePlayers(roomId)
                    activeChannels.add(channel)
                    flow.collect { refreshPlayers() }
                    activeChannels.remove(channel)
                }
                if (isActive) delay(2000)
            }
        }
    }

    private fun observeRoom() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val (channel, flow) = repository.observeRoom(roomId)
                    activeChannels.add(channel)
                    flow.collect {
                        val room = repository.findRoomByCode(_roomCode.value) ?: return@collect
                        val isNewRound = _phase.value == GamePhase.Lobby || room.current_round != _currentRound.value
                        if (room.status == "playing" && room.target_time != null && isNewRound) {
                            _targetTime.value = room.target_time
                            _currentRound.value = room.current_round
                            _phase.value = GamePhase.TargetReveal
                        }
                    }
                    activeChannels.remove(channel)
                }
                if (isActive) delay(2000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        runBlocking {
            activeChannels.forEach { runCatching { repository.unsubscribeChannel(it) } }
            activeChannels.clear()
            runCatching { repository.disconnectRealtime() }
        }
    }

    /** Host-only: rolls a new target and flips the room to "playing", which all
     * clients (including the host) observe as the GO signal via Realtime. */
    fun startRound() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            val target = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
            repository.startRound(roomId, target, _currentRound.value)
        }
    }

    /** Host-only: starts the next round. Does not bump local state directly —
     * the round number is only ever applied once Realtime echoes the DB update,
     * so every device (including the host) detects it as a new round. */
    fun nextRound() {
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            runCatching { repository.resetReady(roomId) }
            val target = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
            repository.startRound(roomId, target, _currentRound.value + 1)
        }
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
        val expectedCount = _players.value.count { !it.eliminated }
        try {
            while (System.currentTimeMillis() < deadline) {
                val results = repository.listResults(roomId, _currentRound.value)
                if (results.size >= expectedCount) break
                delay(500)
            }
        } catch (e: Exception) {
            // timeout or network error — proceed to finishRound with whatever results exist
        }
        finishRound()
    }

    private suspend fun finishRound() {
        val roomId = _roomId.value ?: return
        try {
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

            if (config.gameMode == "timebank") {
                finishTimeBankRound(roomId)
            } else {
                if (results.isNotEmpty()) {
                    val minAbsDelta = results.minOf { kotlin.math.abs(it.delta) }
                    val roundWinnerIds = results
                        .filter { kotlin.math.abs(it.delta) == minAbsDelta }
                        .map { it.playerId }
                    if (_isHost.value) {
                        runCatching { repository.incrementWins(roundWinnerIds) }
                    }
                }
                runCatching { refreshPlayers() }
                _phase.value = if (_players.value.any { it.wins >= config.winTarget }) {
                    GamePhase.Winner
                } else {
                    GamePhase.Results
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Round error: ${e.message}"
            _phase.value = GamePhase.Results
        }
    }

    private suspend fun finishTimeBankRound(roomId: String) {
        val allRows = repository.listAllResults(roomId)

        // compute each player's remaining bank from all results so far
        val updatedPlayers = _players.value.map { player ->
            val totalDelta = allRows
                .filter { it.player_id == player.id }
                .sumOf { kotlin.math.abs(it.delta) }
            val remaining = TimerUtil.round2(config.timeBankSeconds - totalDelta)
            player.copy(bank = remaining, eliminated = remaining <= 0.0)
        }
        _players.value = updatedPlayers

        val alive = updatedPlayers.filter { !it.eliminated }
        _phase.value = when {
            alive.size <= 1 -> GamePhase.Winner
            else -> GamePhase.Results
        }
    }

}

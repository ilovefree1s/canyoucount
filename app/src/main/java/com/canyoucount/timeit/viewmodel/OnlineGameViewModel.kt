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

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver

    private val _chronosPlayerName = MutableStateFlow<String?>(null)
    val chronosPlayerName: StateFlow<String?> = _chronosPlayerName
    private val activeChannels = mutableListOf<RealtimeChannel>()

    fun hostGame(playerName: String, gameConfig: GameConfig = GameConfig()) {
        config = gameConfig
        _gameMode.value = gameConfig.gameMode
        _phase.value = GamePhase.Lobby
        _players.value = emptyList()
        _lastRoundResults.value = emptyList()
        _currentRound.value = 1
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
        _phase.value = GamePhase.Lobby
        _players.value = emptyList()
        _lastRoundResults.value = emptyList()
        _currentRound.value = 1
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
                bank = prev?.bank ?: config.timeBankSeconds,
                eliminated = prev?.eliminated ?: false,
                teamId = it.team_id
            )
        }
    }

    fun updatePlayerTeam(playerId: String, teamId: Int) {
        viewModelScope.launch {
            runCatching { repository.updatePlayerTeam(playerId, teamId) }
            refreshPlayers()
        }
    }

    val gameConfig: GameConfig get() = config

    fun reset() {
        _phase.value = GamePhase.Lobby
        _players.value = emptyList()
        _lastRoundResults.value = emptyList()
        _currentRound.value = 1
        _roomId.value = null
        _roomCode.value = ""
        _isHost.value = false
        _errorMessage.value = null
        _gameMode.value = "standard"
        _isGameOver.value = false
        _chronosPlayerName.value = null
        localRoomPlayerId = null
        val channels = activeChannels.toList()
        activeChannels.clear()
        viewModelScope.launch { channels.forEach { runCatching { it.unsubscribe() } } }
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
    fun restartGame() {
        _isGameOver.value = false
        _lastRoundResults.value = emptyList()
        _players.value = _players.value.map {
            it.copy(wins = 0, bank = config.timeBankSeconds, eliminated = false, ready = false)
        }
        val roomId = _roomId.value ?: return
        viewModelScope.launch {
            runCatching { repository.resetPlayerWins(roomId) }
            runCatching { repository.resetReady(roomId) }
            val target = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
            repository.startRound(roomId, target, 1)
        }
    }

    fun nextRound() {
        _isGameOver.value = false
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

            if (config.gameMode == "timebank") {
                finishTimeBankRound(roomId)
            } else if (config.gameMode == "survival") {
                finishSurvivalRound(results)
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
                val gameOver = if (config.isTeamMode) {
                    // In team mode: a team wins when all its members have wins >= winTarget
                    val teamIds = _players.value.map { it.teamId }.distinct()
                    teamIds.any { tid -> _players.value.filter { it.teamId == tid }.all { it.wins >= config.winTarget } }
                } else {
                    _players.value.any { it.wins >= config.winTarget }
                }
                _isGameOver.value = gameOver
                _phase.value = GamePhase.Results
            }
        } catch (e: Exception) {
            _errorMessage.value = "Round error: ${e.message}"
            _phase.value = GamePhase.Results
        }
    }

    private fun finishSurvivalRound(results: List<RoundResult>) {
        val updatedPlayers = survivalEliminate(_players.value, results)
        _players.value = updatedPlayers
        val alive = updatedPlayers.count { !it.eliminated }
        _isGameOver.value = alive <= 1
        _phase.value = GamePhase.Results
    }

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

    private suspend fun finishTimeBankRound(roomId: String) {
        val allRows = repository.listAllResults(roomId)

        val updatedPlayers = if (config.isTeamMode) {
            // Per team: compute total penalty as sum of worst miss per round per team
            val teamPenaltyByRound = mutableMapOf<Pair<Int, Int>, Double>() // (teamId, round) -> worst miss
            _players.value.forEach { player ->
                allRows.filter { it.player_id == player.id }.forEach { row ->
                    val key = player.teamId to row.round
                    teamPenaltyByRound[key] = maxOf(teamPenaltyByRound[key] ?: 0.0, kotlin.math.abs(row.delta))
                }
            }
            _players.value.map { player ->
                val totalPenalty = teamPenaltyByRound
                    .filterKeys { it.first == player.teamId }
                    .values.sum()
                val remaining = TimerUtil.round2(config.timeBankSeconds - totalPenalty)
                player.copy(bank = remaining, eliminated = remaining <= 0.0)
            }
        } else {
            _players.value.map { player ->
                val totalDelta = allRows
                    .filter { it.player_id == player.id }
                    .sumOf { kotlin.math.abs(it.delta) }
                val remaining = TimerUtil.round2(config.timeBankSeconds - totalDelta)
                player.copy(bank = remaining, eliminated = remaining <= 0.0)
            }
        }
        _players.value = updatedPlayers

        val gameOver = if (config.isTeamMode) teamsAlive(updatedPlayers) <= 1
                       else updatedPlayers.count { !it.eliminated } <= 1
        _isGameOver.value = gameOver
        _phase.value = GamePhase.Results
    }

    private fun teamsAlive(players: List<Player>): Int =
        players.filter { !it.eliminated }.map { it.teamId }.distinct().size

}

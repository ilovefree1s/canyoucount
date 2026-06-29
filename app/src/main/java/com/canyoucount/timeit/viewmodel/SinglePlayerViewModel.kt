package com.canyoucount.timeit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.canyoucount.timeit.data.model.GameConfig
import com.canyoucount.timeit.data.model.RoundResult
import com.canyoucount.timeit.util.SoloLeaderboardStore
import com.canyoucount.timeit.util.SoloScoreEntry
import com.canyoucount.timeit.util.TimerUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class SinglePlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val config = GameConfig()
    private var totalRounds = 3
    val getTotalRounds: Int get() = totalRounds
    private var timeBankStart = 1.0

    private val _phase = MutableStateFlow<GamePhase>(GamePhase.Setup)
    val phase: StateFlow<GamePhase> = _phase

    private val _gameMode = MutableStateFlow("standard")
    val gameMode: StateFlow<String> = _gameMode

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound

    private val _targetTime = MutableStateFlow(0.0)
    val targetTime: StateFlow<Double> = _targetTime

    private val _lastResult = MutableStateFlow<RoundResult?>(null)
    val lastResult: StateFlow<RoundResult?> = _lastResult

    private val _allResults = MutableStateFlow<List<RoundResult>>(emptyList())
    val allResults: StateFlow<List<RoundResult>> = _allResults

    private val _bank = MutableStateFlow(1.0)
    val bank: StateFlow<Double> = _bank

    private var tapStartNanos: Long = 0L

    private fun rollTarget() {
        _targetTime.value = TimerUtil.round2(Random.nextDouble(config.minTime, config.maxTime))
    }

    fun startGame(mode: String, bankSeconds: Double = 1.0, rounds: Int = 3) {
        _gameMode.value = mode
        totalRounds = rounds
        timeBankStart = bankSeconds
        _currentRound.value = 1
        _allResults.value = emptyList()
        _lastResult.value = null
        _bank.value = bankSeconds
        rollTarget()
        _phase.value = GamePhase.TargetReveal
    }

    fun onTargetRevealFinished() { _phase.value = GamePhase.Countdown }
    fun onCountdownGo() { tapStartNanos = TimerUtil.now() }
    fun onCountdownFinished() { _phase.value = GamePhase.Tapping }

    fun onTap() {
        val elapsed = TimerUtil.round2(TimerUtil.elapsedSeconds(tapStartNanos))
        val result = RoundResult(
            playerId = "solo",
            targetTime = _targetTime.value,
            playerTime = elapsed,
            delta = TimerUtil.round2(elapsed - _targetTime.value)
        )
        _lastResult.value = result
        _allResults.value = _allResults.value + result

        if (_gameMode.value == "timebank") {
            val newBank = TimerUtil.round2(_bank.value - kotlin.math.abs(result.delta))
            _bank.value = newBank
            if (newBank <= 0.0) {
                saveTimeBankScore()
                _phase.value = GamePhase.GameOver
                return
            }
        }

        _phase.value = GamePhase.RoundResult
    }

    fun onNextRound() {
        val isLast = _currentRound.value >= totalRounds
        if (_gameMode.value == "standard" && isLast) {
            saveStandardScore()
            _phase.value = GamePhase.Results
        } else {
            _currentRound.value++
            rollTarget()
            _phase.value = GamePhase.TargetReveal
        }
    }

    private fun saveStandardScore() {
        val results = _allResults.value
        if (results.isEmpty()) return
        val avg = results.map { kotlin.math.abs(it.delta) }.average()
        val entry = SoloScoreEntry(
            mode = "standard",
            avgError = TimerUtil.round2(avg),
            rounds = totalRounds,
            startBank = 0.0
        )
        viewModelScope.launch {
            SoloLeaderboardStore.saveStandardScore(getApplication(), entry)
        }
    }

    private fun saveTimeBankScore() {
        val results = _allResults.value
        val survived = (results.size - 1).coerceAtLeast(0)
        val avg = if (results.isEmpty()) 0.0 else results.map { kotlin.math.abs(it.delta) }.average()
        val entry = SoloScoreEntry(
            mode = "timebank",
            avgError = TimerUtil.round2(avg),
            rounds = survived,
            startBank = timeBankStart
        )
        viewModelScope.launch {
            SoloLeaderboardStore.saveTimeBankScore(getApplication(), entry)
        }
    }

    fun restart() { _phase.value = GamePhase.Setup }
}

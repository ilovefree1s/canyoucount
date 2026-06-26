package com.canyoucount.timeit.data.model

data class GameState(
    val config: GameConfig,
    val players: List<Player>,
    val currentRound: Int,
    val results: List<RoundResult>
)

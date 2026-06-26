package com.canyoucount.timeit.data.model

data class RoundResult(
    val playerId: String,
    val targetTime: Double,
    val playerTime: Double,
    val delta: Double
)

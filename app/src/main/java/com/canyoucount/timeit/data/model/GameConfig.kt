package com.canyoucount.timeit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val winTarget: Int = 3,
    val minTime: Double = 1.0,
    val maxTime: Double = 12.0,
    val gameMode: String = "standard", // "standard" or "timebank"
    val timeBankSeconds: Double = 1.0
)

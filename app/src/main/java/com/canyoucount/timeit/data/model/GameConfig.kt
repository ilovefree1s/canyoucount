package com.canyoucount.timeit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val winTarget: Int = 3,
    val minTime: Double = 1.0,
    val maxTime: Double = 10.0,
    val gameMode: String = "standard", // "standard", "timebank", or "survival"
    val timeBankSeconds: Double = 1.0,
    val isTeamMode: Boolean = false,
    val teamCount: Int = 2
)

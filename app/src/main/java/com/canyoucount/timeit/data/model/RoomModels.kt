package com.canyoucount.timeit.data.model

import kotlinx.serialization.Serializable

enum class RoomStatus {
    waiting, playing, finished
}

@Serializable
data class RoomRow(
    val id: String? = null,
    val code: String,
    val host_id: String,
    val status: String = "waiting",
    val config: GameConfig,
    val target_time: Double? = null,
    val current_round: Int = 1
)

@Serializable
data class RoomPlayerRow(
    val id: String? = null,
    val room_id: String,
    val player_name: String,
    val wins: Int = 0
)

@Serializable
data class RoundResultRow(
    val id: String? = null,
    val room_id: String,
    val round: Int,
    val player_id: String,
    val player_time: Double,
    val delta: Double
)

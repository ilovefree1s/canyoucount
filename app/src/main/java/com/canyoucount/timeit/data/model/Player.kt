package com.canyoucount.timeit.data.model

data class Player(
    val id: String,
    val name: String,
    val wins: Int = 0,
    val ready: Boolean = false,
    val bank: Double = 1.0,
    val eliminated: Boolean = false,
    val teamId: Int = 0
)

package com.canyoucount.timeit.util

import kotlin.random.Random

object RoomCodeUtil {
    private const val ALPHABET = "0123456789"

    fun generate(): String =
        (1..6).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")
}

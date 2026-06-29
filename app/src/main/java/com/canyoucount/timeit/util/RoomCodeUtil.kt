package com.canyoucount.timeit.util

import kotlin.random.Random

object RoomCodeUtil {
    fun generate(): String =
        (1..6).map { Random.nextInt(0, 10) }.joinToString("")
}

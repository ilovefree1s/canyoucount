package com.canyoucount.timeit.util

object TimerUtil {
    fun now(): Long = System.nanoTime()

    fun elapsedSeconds(startNanos: Long, endNanos: Long = now()): Double =
        (endNanos - startNanos) / 1_000_000_000.0

    fun round2(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0
}

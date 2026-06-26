package com.canyoucount.timeit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.canyoucount.timeit.ui.theme.GlassOutline
import com.canyoucount.timeit.ui.theme.SandAmber
import kotlinx.coroutines.delay

private const val CYCLE_MILLIS = 12_000L
private const val FRAME_MILLIS = 16L

/**
 * Decorative hourglass. Cycle length (12s) is deliberately decoupled from the
 * target time range (1-15.99s) so it can't be used to estimate elapsed time.
 */
@Composable
fun HourglassAnimation(
    frozen: Boolean,
    modifier: Modifier = Modifier
) {
    var effectiveProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(frozen) {
        if (frozen) return@LaunchedEffect
        var elapsed = (effectiveProgress * CYCLE_MILLIS).toLong()
        while (true) {
            delay(FRAME_MILLIS)
            elapsed = (elapsed + FRAME_MILLIS) % CYCLE_MILLIS
            effectiveProgress = elapsed / CYCLE_MILLIS.toFloat()
        }
    }

    Canvas(modifier = modifier.size(160.dp)) {
        val w = size.width
        val h = size.height
        val neckY = h / 2

        val outline = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.5f, neckY)
            lineTo(w, h)
            lineTo(0f, h)
            lineTo(w * 0.5f, neckY)
            close()
        }
        drawPath(outline, color = GlassOutline, style = Stroke(width = 3f))

        val topFillRatio = (1f - effectiveProgress).coerceIn(0f, 1f)
        val topSandHeight = neckY * topFillRatio
        if (topSandHeight > 1f) {
            val topSand = Path().apply {
                val yStart = neckY - topSandHeight
                val ratio = topSandHeight / neckY
                val xInset = w * 0.5f * (1f - ratio)
                moveTo(xInset, yStart)
                lineTo(w - xInset, yStart)
                lineTo(w * 0.5f, neckY)
                close()
            }
            drawPath(topSand, color = SandAmber)
        }

        val bottomFillRatio = effectiveProgress.coerceIn(0f, 1f)
        val bottomSandHeight = neckY * bottomFillRatio
        if (bottomSandHeight > 1f) {
            val bottomSand = Path().apply {
                val yEnd = neckY + bottomSandHeight
                val ratio = bottomSandHeight / neckY
                val xInset = w * 0.5f * (1f - ratio)
                moveTo(w * 0.5f, neckY)
                lineTo(w - xInset, yEnd)
                lineTo(xInset, yEnd)
                close()
            }
            drawPath(bottomSand, color = SandAmber)
        }

        if (!frozen && effectiveProgress in 0.02f..0.98f) {
            drawLine(
                color = SandAmber,
                start = Offset(w * 0.5f, neckY - 6f),
                end = Offset(w * 0.5f, neckY + 6f),
                strokeWidth = 2f
            )
        }
    }
}

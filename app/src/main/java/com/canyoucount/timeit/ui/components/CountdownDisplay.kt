package com.canyoucount.timeit.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.canyoucount.timeit.ui.theme.SandAmber
import kotlinx.coroutines.delay

/**
 * Full-screen animated 3-2-1-GO! countdown. Calls [onFinished] once GO has displayed.
 */
@Composable
fun CountdownDisplay(
    onTick: (Int) -> Unit = {},
    onFinished: () -> Unit
) {
    var count by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        for (n in 3 downTo 1) {
            count = n
            onTick(n)
            delay(700)
        }
        count = 0 // GO
        onTick(0)
        delay(600)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (scaleIn(animationSpec = tween(200)) + fadeIn()) togetherWith
                    (scaleOut(animationSpec = tween(200)) + fadeOut())
            },
            label = "countdown"
        ) { value ->
            Text(
                text = if (value == 0) "GO!" else value.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = SandAmber
            )
        }
    }
}

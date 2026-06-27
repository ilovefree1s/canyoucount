package com.canyoucount.timeit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.canyoucount.timeit.ui.theme.AccentGreen

/**
 * Full-screen tap-to-start prompt. Player taps when ready; the tap is the GO signal.
 */
@Composable
fun CountdownDisplay(
    onTick: (Int) -> Unit = {},
    onGo: () -> Unit = {},
    onFinished: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                onGo()
                onFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TAP TO START",
            style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 48.sp),
            color = AccentGreen
        )
    }
}

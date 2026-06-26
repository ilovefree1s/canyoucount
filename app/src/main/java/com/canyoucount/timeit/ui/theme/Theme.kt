package com.canyoucount.timeit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TimeItColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    primary = SandAmber,
    onBackground = OnDark,
    onSurface = OnDark,
    onPrimary = BackgroundDark,
    error = AccentRed
)

@Composable
fun TimeItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TimeItColorScheme,
        typography = TimeItTypography,
        content = content
    )
}

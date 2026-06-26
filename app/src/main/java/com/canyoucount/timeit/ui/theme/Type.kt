package com.canyoucount.timeit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TimeItTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 64.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Light, fontSize = 12.sp)
)

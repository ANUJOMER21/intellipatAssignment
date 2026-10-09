package com.example.intellipatassignment.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun style(weight: FontWeight, size: Int, line: Int, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
)

val Typography = Typography(
    headlineMedium = style(FontWeight.SemiBold, 28, 34, (-0.5).sp),
    headlineSmall = style(FontWeight.SemiBold, 24, 30, (-0.4).sp),
    titleLarge = style(FontWeight.SemiBold, 18, 24, (-0.2).sp),
    titleMedium = style(FontWeight.SemiBold, 16, 22, (-0.1).sp),
    bodyLarge = style(FontWeight.Normal, 16, 22),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.Medium, 14, 20, 0.1.sp),
    labelMedium = style(FontWeight.Medium, 12, 16, 0.2.sp),
)

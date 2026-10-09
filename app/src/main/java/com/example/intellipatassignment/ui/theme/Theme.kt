package com.example.intellipatassignment.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9ECF4),
    onPrimaryContainer = Ink,
    secondary = Progress,
    background = Paper,
    onBackground = Text900,
    surface = Color.White,
    onSurface = Text900,
    surfaceVariant = Color(0xFFEFEDE8),
    onSurfaceVariant = Stone500,
    outline = Color(0xFFB9B6AE),
    outlineVariant = Stone200,
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF7A1A12),
    secondaryContainer = Color(0xFFFBF1DC),
    onSecondaryContainer = Color(0xFF6B4410),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE7EAF3),
    onPrimary = Color(0xFF141B2D),
    primaryContainer = Color(0xFF262B3A),
    onPrimaryContainer = Color(0xFFE7EAF3),
    secondary = ProgressDark,
    background = Color(0xFF111214),
    onBackground = Color(0xFFEDEBE6),
    surface = Color(0xFF1A1B1E),
    onSurface = Color(0xFFEDEBE6),
    surfaceVariant = Color(0xFF26272B),
    onSurfaceVariant = Color(0xFF9C9A94),
    outline = Color(0xFF55565B),
    outlineVariant = Color(0xFF2D2E33),
    error = Color(0xFFF2877D),
    errorContainer = Color(0xFF3A1B18),
    onErrorContainer = Color(0xFFF7C2BC),
    secondaryContainer = Color(0xFF3A2D14),
    onSecondaryContainer = Color(0xFFF1D9A0),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun IntellipatAssignmentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

object AppColors {
    val progress @Composable get() = if (isSystemInDarkTheme()) ProgressDark else Progress
    val pending @Composable get() = if (isSystemInDarkTheme()) PendingDark else Pending
}

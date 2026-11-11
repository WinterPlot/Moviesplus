package com.userlab.movieplus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The app is dark-only by design, so a single scheme is used for both system modes.
private val UserLabColorScheme = darkColorScheme(
    primary = UlViolet,
    onPrimary = UlTextPrimary,
    primaryContainer = UlVioletDeep,
    onPrimaryContainer = UlTextPrimary,
    secondary = UlCyan,
    onSecondary = UlMidnight,
    secondaryContainer = UlSurfaceHigh,
    onSecondaryContainer = UlTextPrimary,
    tertiary = UlAmber,
    background = UlMidnight,
    onBackground = UlTextPrimary,
    surface = UlSurface,
    onSurface = UlTextPrimary,
    surfaceVariant = UlSurfaceHigh,
    onSurfaceVariant = UlTextSecondary,
    outline = UlOutline,
    outlineVariant = UlOutline,
    error = UlError,
    onError = UlTextPrimary
)

private val UserLabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // keep brand colours; never use wallpaper colours
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = UserLabColorScheme,
        typography = Typography,
        shapes = UserLabShapes,
        content = content
    )
}

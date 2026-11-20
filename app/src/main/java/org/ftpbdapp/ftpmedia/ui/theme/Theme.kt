package org.ftpbdapp.ftpmedia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D8DF1),
    onPrimary = Color(0xFF241752),
    secondary = Color(0xFF4FD8E0),
    onSecondary = Color(0xFF062E33),
    tertiary = Color(0xFFF2A6C0),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF161A21),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF20262F),
    onSurfaceVariant = Color(0xFF9AA3B5),
    error = Color(0xFFFF8A80),
    outlineVariant = Color(0xFF2A3039)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BC4),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0E7C86),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF191C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C22),
    surfaceVariant = Color(0xFFECEEF3),
    onSurfaceVariant = Color(0xFF5A6270)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

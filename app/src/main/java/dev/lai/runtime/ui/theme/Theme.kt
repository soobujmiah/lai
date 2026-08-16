package dev.lai.runtime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3157D5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FF),
    onPrimaryContainer = Color(0xFF10265F),
    secondary = Color(0xFF196B5A),
    secondaryContainer = Color(0xFFB8F1DD),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EAF1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    primaryContainer = Color(0xFF173B9B),
    secondary = Color(0xFF90D5C0),
    background = Color(0xFF111318),
    surface = Color(0xFF191C22),
)

@Composable
fun LaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

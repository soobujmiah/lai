package dev.lai.runtime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Immutable
data class LaiMotion(
    val fastMs: Int = 120,
    val standardMs: Int = 220,
    val emphasizedMs: Int = 320,
)

val LaiMotionTokens = LaiMotion()

val LocalLaiMotion = staticCompositionLocalOf { LaiMotionTokens }

@Composable
fun LaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}

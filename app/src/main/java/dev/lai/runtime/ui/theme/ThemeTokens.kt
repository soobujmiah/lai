package dev.lai.runtime.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class LaiSpacing(
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 24.dp,
    val xl: androidx.compose.ui.unit.Dp = 32.dp,
)

val LaiSpacingTokens = LaiSpacing()

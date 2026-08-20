package dev.lai.runtime.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/** Motion primitives: purposeful, short, and safe for a performance-sensitive on-device app. */
object LaiMotionSpec {
    val fast: TweenSpec<Float> = tween(120, easing = LinearOutSlowInEasing)
    val standard: TweenSpec<Float> = tween(220, easing = FastOutSlowInEasing)
    val emphasized: TweenSpec<Float> = tween(
        320,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    )
}

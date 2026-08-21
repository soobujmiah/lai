package dev.lai.runtime.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class ShellDestination(val label: String, val glyph: String) {
    Chat("Chat", "C"),
    Models("Models", "M"),
    Workspace("Workspace", "W"),
    Settings("Settings", "S"),
}

/** Presentation-only adaptive shell foundation; existing LAI state/runtime remain authoritative. */
@Composable
fun LaiAppShell(
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit = {},
) {
    var destination by rememberSaveable { mutableStateOf(ShellDestination.Chat) }

    Scaffold(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                ShellDestination.entries.forEach { item ->
                    NavigationRailItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Text(item.glyph) },
                        label = { Text(item.label) },
                    )
                }
            }
            AnimatedContent(
                targetState = destination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "lai-shell-content",
                modifier = Modifier.fillMaxSize(),
            ) { selected -> content(selected.label) }
        }
    }
}

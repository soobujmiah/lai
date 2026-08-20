package dev.lai.runtime.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class ShellDestination(val label: String) {
    Chat("Chat"),
    Models("Models"),
    Workspace("Workspace"),
    Settings("Settings"),
}

@Composable
fun LaiAppShell(
    modifier: Modifier = Modifier,
    content: @Composable (ShellDestination) -> Unit = {},
) {
    var destination by rememberSaveable { mutableStateOf(ShellDestination.Chat) }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                ShellDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    ShellDestination.Chat -> Icons.Outlined.ChatBubbleOutline
                                    ShellDestination.Models -> Icons.Outlined.SmartToy
                                    ShellDestination.Workspace -> Icons.Outlined.FolderOpen
                                    ShellDestination.Settings -> Icons.Outlined.Settings
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
            AnimatedContent(
                targetState = destination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "lai-shell-content",
                modifier = Modifier.fillMaxSize(),
            ) { selected ->
                content(selected)
            }
        }
    }
}

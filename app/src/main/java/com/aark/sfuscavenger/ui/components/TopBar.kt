package com.aark.sfuscavenger.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    showLeaveGame: Boolean = false,
    onLeaveGame: (() -> Unit)? = null,
    showSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,

    // re-join button
    showRejoinGame: Boolean = false,
    onRejoinGame: (() -> Unit)? = null,

    searchContent: (@Composable () -> Unit)? = null,
) {
    TopAppBar(
        modifier = modifier,
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Maroon,
            titleContentColor = White,
            actionIconContentColor = White,
        ),
        title = {
            if (searchContent != null) {
                searchContent()
            } else {
                Text(
                    text = title,
                    color = White
                )
            }
        },
        actions = {
            // Re-join button
            if (showRejoinGame && onRejoinGame != null) {
                TextButton(onClick = onRejoinGame) {
                    Text(text = "Re-join game", color = White)
                }
            }

            if (showSettings && onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Profile settings",
                        tint = White
                    )
                }
            }

            if (showLeaveGame && onLeaveGame != null) {
                TextButton(onClick = onLeaveGame) {
                    Text(text = "Leave game", color = White)
                }
            }
        }
    )
}


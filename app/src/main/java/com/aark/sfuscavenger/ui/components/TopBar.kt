package com.aark.sfuscavenger.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    searchContent: (@Composable () -> Unit)? = null,
    ) {
//    Column {
    TopAppBar(
        modifier = modifier,
        windowInsets = WindowInsets(0),
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
//    }

}
package com.aark.sfuscavenger.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

data class GameNavItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun GameBottomNavBar(
    navController: NavHostController,
    hasUnreadChat: Boolean
) {
    val items = listOf(
        GameNavItem("Tasks", "tasks", Icons.Filled.ListAlt),
        GameNavItem("Map", "map", Icons.Filled.Map),
        GameNavItem("Leaderboard", "leaderboard", Icons.Filled.Leaderboard),
        GameNavItem("Chat", "chat", Icons.Filled.MailOutline)
    )

    NavigationBar(containerColor = Color(0xFFA6192E)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route)
                    }
                },
                icon = {
                    if (item.route == "chat" && hasUnreadChat) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color.White,
                                    modifier = Modifier.scale(0.9f),
                                )
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = Color.White
                        )
                    }
                },
                label = { Text(text = item.label, color = Color.White) }
            )
        }
    }
}

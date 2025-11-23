package com.aark.sfuscavenger.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aark.sfuscavenger.R
import com.aark.sfuscavenger.ui.theme.White

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: @Composable (selected: Boolean) -> Unit
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", "home") { selected ->
            Icon(imageVector = Icons.Filled.Home, contentDescription = "Home", tint = if(selected) Color.Unspecified else White)
        },
        BottomNavItem("Events", "events") { selected ->
            Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Events", tint = if(selected) Color.Unspecified else White)
        },
        BottomNavItem("History", "history") { selected ->
            Icon(
                painter = painterResource(R.drawable.sc_history_48dp), contentDescription = "History", tint = if(selected) Color.Unspecified else White
            )
        },
        BottomNavItem("Profile", "profile") { selected ->
            Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile", tint = if(selected) Color.Unspecified else White)
        }
    )

    NavigationBar(containerColor = Color(0xFFA6192E)) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route)
                    }
                },
                icon = {
//                    Icon(
//                        imageVector = item.icon,
//                        contentDescription = item.label,
//                        tint = Color.White
//                    )
                    item.icon(selected)
                },
                label = { Text(text = item.label, color = White) }
            )
        }
    }
}
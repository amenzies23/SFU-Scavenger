package com.aark.sfuscavenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.ui.BottomNavBar
import com.aark.sfuscavenger.ui.home.HomeScreen
import com.aark.sfuscavenger.ui.lobby.LobbyScreen
import com.aark.sfuscavenger.ui.create.CreateScreen
import com.aark.sfuscavenger.ui.join.JoinScreen
import com.aark.sfuscavenger.ui.social.SocialScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SFUScavengerApp()
        }
    }
}

@Composable
fun SFUScavengerApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            if (showBottomNavBar(navController)) {
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("create") { CreateScreen(navController) }
            composable("join") { JoinScreen(navController) }
            composable("social") { SocialScreen(navController) }
            composable("lobby") { LobbyScreen(navController) }
        }
    }
}

// Determines which screens will have the bottom navigation bar
@Composable
fun showBottomNavBar(navController: NavHostController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return currentRoute in listOf("home", "create", "join", "social")
}

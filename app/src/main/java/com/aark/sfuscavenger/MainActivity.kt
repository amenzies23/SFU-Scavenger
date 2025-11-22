package com.aark.sfuscavenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.ui.BottomNavBar
import com.aark.sfuscavenger.ui.events.EventsScreen
import com.aark.sfuscavenger.ui.lobby.LobbyScreen
import com.aark.sfuscavenger.ui.profile.ProfileScreen
import com.aark.sfuscavenger.ui.home.HomeScreen
import com.aark.sfuscavenger.ui.login.SignInScreen
import com.aark.sfuscavenger.ui.login.SignUpScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.view.WindowInsetsControllerCompat
import com.aark.sfuscavenger.ui.login.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aark.sfuscavenger.ui.components.TopBar
import com.aark.sfuscavenger.ui.history.HistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make status bar icons dark
//        val wic = WindowInsetsControllerCompat(window, window.decorView)
//        wic.isAppearanceLightStatusBars = true

        installSplashScreen()
        setContent {
            SFUScavengerApp()
        }
    }
}

@Composable
fun SFUScavengerApp() {
    val navController = rememberNavController()
    val vm: AuthViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var showProfileSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            navController.navigate("home") { popUpTo("login") { inclusive = true } }
        } else {
            navController.navigate("login") { popUpTo(0) }
        }
    }

    // Tracking the current route to display in the TopBar component
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topBarTitle: String? = when {
        currentRoute == "home" -> "Home"
        currentRoute == "events" -> "Events"
        currentRoute == "history" -> "History"
        currentRoute == "profile" -> "Profile"
        currentRoute?.startsWith("lobby/") == true -> "Lobby"
        else -> null
    }

    Scaffold(
        topBar = {
            if (topBarTitle != null) {
                TopBar(
                    title = topBarTitle,
                    showSettings = (currentRoute == "profile"),
                    onSettingsClick = { showProfileSettings = true }
                )
            }
        },
        bottomBar = {
            if (showBottomNavBar(navController)) {
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        MaterialTheme {
            Surface {
                NavHost(
                    navController = navController,
                    startDestination = "login",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("login") {
                        SignInScreen(
                            navController,
                            loading = state.loading,
                            error = state.error,
                            onLogin = { e, p -> vm.signIn(e, p) },
                            onGoToSignUp = { navController.navigate("signup") }
                        )
                    }
                    composable("signup") {
                        SignUpScreen(
                            navController,
                            loading = state.loading,
                            error = state.error,
                            onSignUp = { e, p -> vm.signUp(e, p) },
                            onGoToLogin = { navController.popBackStack() }
                        )
                    }
                    composable("home") { HomeScreen(navController) }
                    composable("events") { EventsScreen(navController) }
                    composable("history") { HistoryScreen(navController) }
                    composable("profile") {
                        ProfileScreen(
                            navController = navController,
                            showSettings = showProfileSettings,
                            onRequestCloseSettings = { showProfileSettings = false }
                        )
                    }
                    composable(
                        route = "lobby/{gameId}"
                    ) { backStackEntry ->
                        val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
                        LobbyScreen(navController, gameId)
                    }

                }
            }
        }
    }
}

// Determines which screens will have the bottom navigation bar
@Composable
fun showBottomNavBar(navController: NavHostController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return currentRoute in listOf("home", "events", "history", "profile")
}

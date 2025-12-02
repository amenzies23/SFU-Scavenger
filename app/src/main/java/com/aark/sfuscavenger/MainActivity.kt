package com.aark.sfuscavenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.aark.sfuscavenger.ui.events.CreateGameScreen
import com.aark.sfuscavenger.ui.history.HistoryScreen
import com.aark.sfuscavenger.ui.history.HistorySearchBar
import com.aark.sfuscavenger.ui.history.HistoryViewModel
import com.aark.sfuscavenger.ui.history.ResultsScreen
import com.aark.sfuscavenger.ui.history.PlacementScreen
import com.aark.sfuscavenger.ui.chat.ChatScreen
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.aark.sfuscavenger.GameActivity
import com.aark.sfuscavenger.ui.home.HomeScreen
import com.aark.sfuscavenger.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val isLoggedIn = state.isLoggedIn

    val homeViewModel: HomeViewModel? =
        if (isLoggedIn) viewModel(key = "home") else null

    val historyViewModel: HistoryViewModel? =
        if (isLoggedIn) viewModel(key = "history") else null

    val topBarTitle: String? = when {
        currentRoute == "home" -> "Home"
        currentRoute == "events" -> "Events"
        currentRoute == "history" -> "History"
        currentRoute == "profile" -> "Profile"
        currentRoute?.startsWith("lobby/") == true -> "Lobby"
        currentRoute?.startsWith("placement/") == true -> "Placements"
        currentRoute?.startsWith("createGame") == true -> {
            if (currentRoute.contains("gameId=")) "Edit Game" else "Create Game"
        }
        else -> null
    }

    val isLobbyRoute = currentRoute?.startsWith("lobby/") == true

    Scaffold(
        topBar = {
            if (topBarTitle != null) {
                val shouldShowRejoin =
                    isLoggedIn && (currentRoute == "home" || currentRoute == "events")

                val activeGame =
                    if (shouldShowRejoin) homeViewModel?.liveGamesForUser?.firstOrNull()
                    else null

                TopBar(
                    title = topBarTitle,
                    showSettings = (currentRoute == "profile"),
                    onSettingsClick = { showProfileSettings = true },

                    // re-join button
                    showRejoinGame = activeGame != null,
                    onRejoinGame = activeGame?.let { game ->
                        {
                            val intent = Intent(context, GameActivity::class.java).apply {
                                putExtra("gameId", game.id)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        },
        bottomBar = {
            when {
                showBottomNavBar(navController) -> BottomNavBar(navController)
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
                    composable("home") {
                        val hv = homeViewModel ?: viewModel(key = "home")
                        HomeScreen(navController, vm = hv)
                    }
                    composable("events") { EventsScreen(navController) }
                    composable("history") {
                        val hv = historyViewModel ?: viewModel(key = "history")
                        HistoryScreen(
                            navController = navController,
                            viewModel = hv
                        )
                    }
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
                    composable(
                        route = "createGame?gameId={gameId}"
                    ) { backStackEntry ->
                        val gameId = backStackEntry.arguments?.getString("gameId")
                        CreateGameScreen(navController, gameId = gameId) }
                    composable(
                        route = "results/{gameId}/{teamId}"
                    ) { backStackEntry ->
                        val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
                        val teamArg = backStackEntry.arguments?.getString("teamId").orEmpty()
                        val teamId = teamArg.takeUnless { it == "none" }
                        ResultsScreen(navController, gameId, teamId)
                    }
                    composable(
                        route = "placement/{gameId}"
                    ) { backStackEntry ->
                        val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
                        PlacementScreen(navController, gameId)
                    }
                    composable(
                        route = "chat/{gameId}"
                    ) { backStackEntry ->
                        val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
                        ChatScreen(gameId = gameId)
                    }
                }
            }
        }
    }

    LaunchedEffect(state.isLoggedIn, currentRoute) {
        if (currentRoute != null) {
            val isAuthenticatedRoute = currentRoute == "home" || 
                currentRoute == "events" || 
                currentRoute == "history" || 
                currentRoute == "profile" || 
                currentRoute.startsWith("lobby/") || 
                currentRoute.startsWith("results/") ||
                currentRoute.startsWith("placement/") ||
                currentRoute.startsWith("chat/") ||
                currentRoute.startsWith("createGame")
            
            if (state.isLoggedIn && !isAuthenticatedRoute) {
                navController.navigate("home") { 
                    popUpTo("login") { inclusive = true } 
                }
            } else if (!state.isLoggedIn && currentRoute != "login" && currentRoute != "signup") {
                navController.navigate("login") { 
                    popUpTo(0) 
                }
            }
        }
    }
}

@Composable
fun showBottomNavBar(navController: NavHostController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return currentRoute in listOf("home", "events", "history", "profile")
}

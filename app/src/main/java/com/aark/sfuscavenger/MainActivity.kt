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
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.ui.BottomNavBar
import com.aark.sfuscavenger.ui.create.CreateScreen
import com.aark.sfuscavenger.ui.join.JoinScreen
import com.aark.sfuscavenger.ui.lobby.LobbyScreen
import com.aark.sfuscavenger.ui.social.SocialScreen
import com.aark.sfuscavenger.ui.home.HomeScreen
import com.aark.sfuscavenger.ui.login.SignIn
import com.aark.sfuscavenger.ui.login.SignInScreen
import com.aark.sfuscavenger.ui.login.SignUpScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

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
    val vm: com.aark.sfuscavenger.ui.login.AuthViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            navController.navigate("home") { popUpTo("login") { inclusive = true } }
        } else {
            navController.navigate("login") { popUpTo(0) }
        }
    }

    Scaffold(
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
                    composable("create") { CreateScreen(navController) }
                    composable("join") { JoinScreen(navController) }
                    composable("social") { SocialScreen(navController) }
                    composable("lobby") { LobbyScreen(navController) }
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
    return currentRoute in listOf("home", "create", "join", "social")
}

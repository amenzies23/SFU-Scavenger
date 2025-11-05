package com.aark.sfuscavenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    MaterialTheme {
        Surface {
            NavHost(navController, startDestination = "login") {
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
                composable("home") { HomeScreen() }
            }
        }
    }
}
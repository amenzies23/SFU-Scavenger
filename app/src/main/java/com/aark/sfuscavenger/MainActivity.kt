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
    MaterialTheme {
        Surface {
            NavHost(navController, startDestination = "login") {
                composable("login") { SignInScreen(navController) }
                composable("signup") { SignUpScreen(navController) }
                composable("home") { HomeScreen() }
            }
        }
    }
}
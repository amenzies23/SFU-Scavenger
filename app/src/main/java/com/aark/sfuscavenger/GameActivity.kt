package com.aark.sfuscavenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.ui.GameBottomNavBar
import com.aark.sfuscavenger.ui.chat.ChatBadgeViewModel
import com.aark.sfuscavenger.ui.chat.ChatScreen
import com.aark.sfuscavenger.ui.components.TopBar
import com.aark.sfuscavenger.ui.leaderboard.LeaderboardScreen
import com.aark.sfuscavenger.ui.map.MapScreen
import com.aark.sfuscavenger.ui.tasks.TaskScreen
import androidx.compose.foundation.layout.consumeWindowInsets

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getStringExtra("gameId") ?: ""

        if (gameId.isBlank()) {
            finish()
            return
        }

        setContent {
            GameApp(
                gameId = gameId,
                onExitGame = {
                    // Finish GameActivity and go back to the MainActivity
                    finish()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            )
        }
    }
}

@Composable
fun GameApp(
    gameId: String,
    onExitGame: () -> Unit
) {
    val navController = rememberNavController()

    val chatBadgeVm: ChatBadgeViewModel = viewModel()
    val hasUnreadChat by chatBadgeVm.hasUnreadChat.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        chatBadgeVm.start(gameId)
    }

    // Track current route for dynamic top bar title
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "tasks"
    val topTitle = when (currentRoute) {
        "tasks" -> "Tasks"
        "map" -> "Map"
        "leaderboard" -> "Leaderboard"
        "chat" -> "Chat"
        else -> "Game"
    }

    Scaffold(
        topBar = {
            TopBar(
                title = topTitle,
                showLeaveGame = true,
                onLeaveGame = onExitGame
            )
        },
        bottomBar = {
            GameBottomNavBar(
                navController = navController,
                hasUnreadChat = hasUnreadChat
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        MaterialTheme {
            Surface {
                GameNavHost(
                    navController = navController,
                    gameId = gameId,
                    modifier = Modifier
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun GameNavHost(
    navController: NavHostController,
    gameId: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = modifier
    ) {
        composable("tasks") {
            TaskScreen(gameId = gameId)
        }
        composable("map") {
            MapScreen(gameId = gameId)
        }
        composable("leaderboard") {
            LeaderboardScreen(gameId = gameId)
        }
        composable("chat") {
            ChatScreen(gameId = gameId)
        }
    }
}

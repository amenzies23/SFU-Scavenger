package com.aark.sfuscavenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aark.sfuscavenger.ui.history.ResultsScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

    // Observe game status to navigate to results when game ends
    var gameStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gameId) {
        chatBadgeVm.start(gameId)
    }

    // Listen to game status changes
    DisposableEffect(gameId) {
        val gameRef = FirebaseFirestore.getInstance()
            .collection("games")
            .document(gameId)

        val listener = gameRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val status = snapshot.getString("status")
            val previousStatus = gameStatus
            gameStatus = status

            // Navigate to results when game ends
            if (previousStatus != null && previousStatus != "ended" && status == "ended") {
                navController.navigate("results") {
                    popUpTo("tasks") { inclusive = false }
                }
            }
        }

        onDispose {
            listener.remove()
        }
    }

    // Track current route for dynamic top bar title
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "tasks"
    val topTitle = when (currentRoute) {
        "tasks" -> "Tasks"
        "map" -> "Map"
        "leaderboard" -> "Leaderboard"
        "chat" -> "Chat"
        "results" -> "Results"
        else -> "Game"
    }

    // Hide bottom nav bar on results screen
    val showBottomBar = currentRoute != "results"

    Scaffold(
        topBar = {
            TopBar(
                title = topTitle,
                showLeaveGame = currentRoute != "results",
                onLeaveGame = onExitGame
            )
        },
        bottomBar = {
            if (showBottomBar) {
                GameBottomNavBar(
                    navController = navController,
                    hasUnreadChat = hasUnreadChat
                )
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        MaterialTheme {
            Surface {
                GameNavHost(
                    navController = navController,
                    gameId = gameId,
                    onExitGame = onExitGame,
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
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = modifier
    ) {
        composable("tasks") {
            TaskScreen(
                gameId = gameId,
                onEndGame = {
                    navController.navigate("results") {
                        popUpTo("tasks") { inclusive = false }
                    }
                }
            )
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
        composable("results") {
            // Fetch the user's team ID
            var userTeamId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(gameId) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val teamsSnap = FirebaseFirestore.getInstance()
                        .collection("games")
                        .document(gameId)
                        .collection("teams")
                        .get()
                        .await()

                    // Find which team the user is in
                    for (teamDoc in teamsSnap.documents) {
                        val memberSnap = teamDoc.reference
                            .collection("members")
                            .document(uid)
                            .get()
                            .await()

                        if (memberSnap.exists()) {
                            userTeamId = teamDoc.id
                            break
                        }
                    }
                }
            }

            ResultsScreen(
                navController = navController,
                gameId = gameId,
                teamId = userTeamId,
                onBackToHome = onExitGame
            )
        }
    }
}
package com.aark.sfuscavenger.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.aark.sfuscavenger.data.models.User
import com.aark.sfuscavenger.repositories.GameRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import com.aark.sfuscavenger.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.CustomText

@Composable
fun ResultsScreen(
    navController: NavController,
    gameId: String,
    teamId: String?,
    onBackToHome: () -> Unit = {}
) {
    val gameRepository = remember { GameRepository() }
    val teamRepository = remember { TeamRepository() }
    val userRepository = remember { UserRepository() }
    val auth = remember { FirebaseAuth.getInstance() }

    var gameName by remember { mutableStateOf<String?>(null) }
    var placement by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableStateOf<Int?>(null) }
    var teamMembers by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(gameId, teamId) {
        val game = gameRepository.getGame(gameId)
        gameName = game?.name?.ifBlank { "Untitled Game" }

        if (teamId != null && teamId != "none") {
            // Get team summary for placement and score
            val teamSummary = teamRepository.getTeamSummary(gameId, teamId)
            val placementInt = teamSummary?.placement ?: 0
            placement = if (placementInt > 0) {
                when (placementInt) {
                    1 -> "1st place"
                    2 -> "2nd place"
                    3 -> "3rd place"
                    else -> "${placementInt}th place"
                }
            } else {
                "N/A"
            }
            score = teamSummary?.score

            // Get all team members
            val membersMap = teamRepository.getTeamMembersWithUserObject(gameId, teamId)
            teamMembers = membersMap.values.toList()
        } else {
            // No team - just show the current user
            placement = "N/A"
            score = null
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                val currentUser = userRepository.fetchUserById(currentUserId)
                teamMembers = if (currentUser != null) listOf(currentUser) else emptyList()
            } else {
                teamMembers = emptyList()
            }
        }
    }

    val background = Brush.verticalGradient(
        listOf(Color(0xFFF7F1EA), Color(0xFFF1E5DB))
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        color = Color.Transparent
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ResultsTopBar(
                    title = gameName ?: "Game summary",
                    onBack = onBackToHome
                )
            }

            item {
                InfoCard {
                    Text(
                        text = "Game ID: $gameId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Team: ${teamId?.takeIf { it != "none" } ?: "Not assigned"}",
                        color = Maroon
                    )
                    Text(text = "Placement: ${placement ?: "N/A"}")
                    Text(text = "Score: ${score?.let { "$it pts" } ?: "N/A"}")
                }
            }

            item { SectionTitle(if (teamId != null && teamId != "none") "Team members" else "Player") }
            item {
                InfoCard {
                    if (teamMembers.isEmpty()) {
                        Text(
                            text = "No members found",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            teamMembers.forEach { user ->
                                TeamMemberRow(user = user)
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Task progress") }
            item {
                InfoCard {
                    Text(
                        text = "Task progress will appear here once implemented.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { SectionTitle("Notes") }
            item {
                InfoCard {
                    Text(
                        text = "Additional notes will appear here once implemented.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = Color.White
                    )
                ) {
                    Text("Back to Home")
                }
            }
        }
    }
}

@Composable
private fun ResultsTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }
        CustomText(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFD7C3B5)), shape)
            .clip(shape),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = Color(0xFFFDF8F2)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    CustomText(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun TeamMemberRow(user: User) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.email,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (user.username != null) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        CustomText("Level ${user.level}")
    }
}
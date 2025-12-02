package com.aark.sfuscavenger.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aark.sfuscavenger.data.models.User
import com.aark.sfuscavenger.data.models.Task
import com.aark.sfuscavenger.data.models.Submission
import com.aark.sfuscavenger.repositories.GameRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import com.aark.sfuscavenger.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.ScavengerText
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Locale

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
    val firestore = remember { FirebaseFirestore.getInstance() }

    var gameName by remember { mutableStateOf<String?>(null) }
    var placementDisplay by remember { mutableStateOf<PlacementDisplay?>(null) }
    var score by remember { mutableStateOf<Int?>(null) }
    var teamMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    var completedTasks by remember { mutableStateOf<List<CompletedTaskResult>>(emptyList()) }
    var completedTasksLoading by remember { mutableStateOf(false) }
    var completedTasksError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gameId, teamId) {
        val game = gameRepository.getGame(gameId)
        gameName = game?.name?.ifBlank { "Untitled Game" }

        if (teamId != null && teamId != "none") {
            val teamSummary = teamRepository.getTeamSummary(gameId, teamId)
            val placementInt = teamSummary?.placement ?: 0
            placementDisplay = placementInt.toPlacementDisplay()
            score = teamSummary?.score
            
            val membersMap = teamRepository.getTeamMembersWithUserObject(gameId, teamId)
            teamMembers = membersMap.values.toList()

            completedTasksLoading = true
            completedTasksError = null
            completedTasks = try {
                fetchCompletedTasks(gameId, teamId, firestore)
            } catch (e: Exception) {
                completedTasksError = e.message ?: "Failed to load completed tasks."
                emptyList()
            } finally {
                completedTasksLoading = false
            }
        } else {
            placementDisplay = null
            score = null
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                val currentUser = userRepository.fetchUserById(currentUserId)
                teamMembers = if (currentUser != null) listOf(currentUser) else emptyList()
            }
            completedTasks = emptyList()
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
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 16.dp, 
                        bottom = 80.dp // Space for the fixed button area
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ResultsTopBar(
                            title = gameName ?: "Game summary",
                            onBack = { navController.popBackStack() }
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
                            PlacementText(display = placementDisplay)
                            Text(text = "Score: ${score?.let { "$it pts" } ?: "N/A"}")
                        }
                    }

                    item { 
                        SectionTitle(if (teamId != null && teamId != "none") "Team members" else "Player") 
                    }
                    
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
                            when {
                                completedTasksLoading -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = Maroon)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Loading completed tasks…", color = Color.Gray)
                                    }
                                }
                                completedTasksError != null -> {
                                    Text(
                                        text = completedTasksError ?: "Failed to load tasks.",
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                completedTasks.isEmpty() -> {
                                    Text(
                                        text = "No tasks completed yet.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                else -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        completedTasks.forEach { task ->
                                            CompletedTaskResultCard(task)
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // Fixed bottom section
            BottomActionButtons(
                onNavigateLeaderboard = { navController.navigate("placement/$gameId") },
                onBackHome = {
                    navController.popBackStack()
                    onBackToHome()
                }
            )
        }
    }
}
@Composable
private fun BottomActionButtons(
    onNavigateLeaderboard: () -> Unit,
    onBackHome: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateLeaderboard,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("View Leaderboard")
            }

            OutlinedButton(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Maroon),
                border = BorderStroke(1.dp, Maroon)
            ) {
                Text("Back to Home")
            }
        }
    }
}

data class PlacementDisplay(
    val label: String,
    val color: Color,
    val icon: String
)

private fun Int.toPlacementDisplay(): PlacementDisplay? {
    if (this <= 0) return null
    return when (this) {
        1 -> PlacementDisplay("1st place", Color(0xFFD4AF37), "\uD83E\uDD47") // 🥇
        2 -> PlacementDisplay("2nd place", Color(0xFFC0C0C0), "\uD83E\uDD48") // 🥈
        3 -> PlacementDisplay("3rd place", Color(0xFFCD7F32), "\uD83E\uDD49") // 🥉
        else -> PlacementDisplay("${this}th place", Maroon, "\uD83C\uDFC5")
    }
}

@Composable
private fun PlacementText(display: PlacementDisplay?) {
    if (display == null) {
        Text(text = "Placement: N/A")
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = display.icon,
            fontSize = MaterialTheme.typography.titleMedium.fontSize
        )
        Text(
            text = display.label,
            color = display.color,
            fontWeight = FontWeight.Bold
        )
    }
}


data class CompletedTaskResult(
    val taskName: String,
    val description: String,
    val points: Int,
    val type: String,
    val submittedText: String?,
    val photoPath: String?,
    val submittedAt: com.google.firebase.Timestamp?
)

private suspend fun fetchCompletedTasks(
    gameId: String,
    teamId: String,
    firestore: FirebaseFirestore
): List<CompletedTaskResult> {
    val tasksSnap = firestore.collection("games")
        .document(gameId)
        .collection("tasks")
        .get()
        .await()

    val tasks = tasksSnap.documents.mapNotNull { doc ->
        doc.toObject(Task::class.java)?.copy(id = doc.id)
    }.associateBy { it.id }

    val submissionsSnap = firestore.collection("games")
        .document(gameId)
        .collection("teams")
        .document(teamId)
        .collection("submissions")
        .get()
        .await()

    return submissionsSnap.documents.mapNotNull { doc ->
        val submission = doc.toObject(Submission::class.java) ?: return@mapNotNull null
        val approved = submission.status == "approved" || submission.status == "auto_approved"
        if (!approved) return@mapNotNull null
        val task = tasks[submission.taskId] ?: return@mapNotNull null

        CompletedTaskResult(
            taskName = task.name,
            description = task.description,
            points = task.points,
            type = submission.type,
            submittedText = submission.text,
            photoPath = submission.mediaStoragePath,
            submittedAt = submission.createdAt
        )
    }.sortedBy { it.taskName.lowercase(Locale.getDefault()) }
}

@Composable
private fun CompletedTaskResultCard(result: CompletedTaskResult) {
    val formatter = remember {
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Task: ${result.taskName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Points: ${result.points}",
            color = Maroon,
            fontWeight = FontWeight.Medium
        )
        if (result.description.isNotBlank()) {
            Text(
                text = result.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        result.submittedAt?.toDate()?.let { date ->
            Text(
                text = "Submitted at: ${formatter.format(date)}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "Answer (${result.type.uppercase()}):",
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        when {
            result.type == "photo" && !result.photoPath.isNullOrBlank() -> {
                SubmittedPhoto(path = result.photoPath)
            }
            !result.submittedText.isNullOrBlank() -> {
                Text(
                    text = result.submittedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
            else -> {
                Text(
                    text = "No answer text available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
        Divider(color = Color(0xFFE1D5CD))
    }
}

@Composable
private fun SubmittedPhoto(path: String) {
    var photoUrl by remember(path) { mutableStateOf<String?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        error = null
        photoUrl = null
        try {
            val url = FirebaseStorage.getInstance()
                .reference
                .child(path)
                .downloadUrl
                .await()
                .toString()
            photoUrl = url
        } catch (e: Exception) {
            error = "Unable to load photo."
        }
    }

    when {
        photoUrl != null -> {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Submitted photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        error != null -> {
            Text(
                text = error ?: "",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = Maroon)
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        ScavengerText(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 8.dp),
            color = Color.Black
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
    ScavengerText(
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
        ScavengerText("Level ${user.level}")
    }
}

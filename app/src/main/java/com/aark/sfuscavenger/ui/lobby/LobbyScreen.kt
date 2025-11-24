package com.aark.sfuscavenger.ui.lobby

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.aark.sfuscavenger.GameActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White

@Composable
fun LobbyScreen(
    navController: NavController,
    gameId: String,
    viewModel: LobbyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(gameId) {
        viewModel.startObserving(gameId)
    }
    // For launching GameActivity when the game status changes to started
    LaunchedEffect(state.gameStatus) {
        if (state.gameStatus == "started" && state.gameId != null) {
            val intent = Intent(context, GameActivity::class.java).apply {
                putExtra("gameId", state.gameId)
            }
            context.startActivity(intent)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF3ECE7)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color(0xFFF3ECE7),
                            0.5f to Beige,
                            1.0f to Beige
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                LobbyTopBar(
                    isHost = state.isHost,
                    gameName = state.gameName,
                    joinCode = state.joinCode
                )


                Spacer(modifier = Modifier.height(8.dp))

                if (state.loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (state.error != null) {
                        Text(
                            text = state.error ?: "",
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    LobbyContent(
                        state = state,
                        onJoinTeam = { viewModel.joinTeam(it) },
                        onCreateTeam = { viewModel.createTeam(it) },
                        onLeaveTeam = { viewModel.leaveTeam() },
                        onDeleteTeam = { viewModel.deleteTeam() },
                        onStartGame = { viewModel.startGame() }

                    )
                }
            }

            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "Lobby chat",
                tint = Maroon,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun LobbyTopBar(
    isHost: Boolean,
    gameName: String,
    joinCode: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = if (isHost) "Lobby (Host)" else "Lobby",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )

            Text(
                text = gameName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )

            if (isHost && joinCode.isNotBlank()) {
                Text(
                    text = "Join Code: $joinCode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA46A4B)
                )
            } else if (!isHost) {
                Text(
                    text = "Waiting for host to start...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA46A4B)
                )
            }
        }
    }
}


@Composable
private fun LobbyContent(
    state: LobbyUiState,
    onJoinTeam: (String) -> Unit,
    onCreateTeam: (String) -> Unit,
    onLeaveTeam: () -> Unit,
    onStartGame: () -> Unit,
    onDeleteTeam: () -> Unit,

    ) {
    val totalPlayers = state.teams.sumOf { it.members.size }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        if (!state.isHost) {
            Text(
                text = if (state.currentUserTeamId == null)
                    "Choose a team or create one:"
                else
                    "You are in a team. You can still switch or leave before the game starts.",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Text(
                text = "Teams in this lobby:",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.teams) { team ->
                TeamCard(
                    team = team,
                    isSelected = team.id == state.currentUserTeamId,
                    isHost = state.isHost,
                    canJoin = !state.isHost && state.gameStatus == "live",
                    onClick = { onJoinTeam(team.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$totalPlayers players joined",
            fontSize = 14.sp,
            color = Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // CREATE TEAM BUTTON
            if (!state.isHost) {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Maroon
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                ) {
                    Text("Create Team", fontWeight = FontWeight.Bold)
                }
            }


            // If Host Lobby view, start game button should be available
            if (state.isHost) {
                Button(
                    onClick = onStartGame,
                    enabled = state.gameStatus == "live" && totalPlayers > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = White
                    )
                ) {
                    Text(
                        text = if (state.gameStatus == "started") "Game Started" else "Start Game"
                    )
                }
            }
        }

        // Leave team button
        if (!state.isHost && state.currentUserTeamId != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLeaveTeam,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Maroon
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            ) {
                Text("Leave Team", fontWeight = FontWeight.Bold)
            }
        }

        // DELETE TEAM button  (only visible if user is ONLY member)
        if (!state.isHost && state.currentUserTeamId != null) {
            val userTeam = state.teams.firstOrNull { it.id == state.currentUserTeamId }
            val isOnlyMember = (userTeam?.members?.size == 1)

            if (isOnlyMember) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onDeleteTeam() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                ) {
                    Text("Delete Team", fontWeight = FontWeight.Bold)
                }
            }
        }

    }

    if (showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateTeam(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TeamCard(
    team: LobbyTeamUi,
    isSelected: Boolean,
    isHost: Boolean,
    canJoin: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) Color(0xFFFFF4EC) else Color(0xFFD3C5BB)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                enabled = canJoin,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = team.name.ifBlank { "Untitled Team" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelected && !isHost) {
                    Text(
                        text = "Joined",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Maroon
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (team.members.isEmpty()) {
                Text(
                    "No players yet",
                    fontSize = 14.sp,
                    color = Black.copy(alpha = 0.7f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    team.members.forEach { name ->
                        Text("• $name", fontSize = 14.sp, color = Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Team") },
        text = {
            Column {
                Text("Give your team a name")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    singleLine = true,
                    label = { Text("Team name") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (teamName.isNotBlank()) onConfirm(teamName) },
                enabled = teamName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.aark.sfuscavenger.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White
import androidx.compose.material.icons.filled.Close


/**
 * TaskScreen handles both the player and host workflows for tasks
 * Players can view tasks, submit answers, and check their progress
 * Hosts can review submissions and approve/reject them
 */

@Composable
fun TaskScreen(
    gameId: String,
    vm: TaskViewModel = viewModel()
) {

    // Grab the latest UI state from the ViewModel
    // re-composes whenever state changes
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedTask by remember { mutableStateOf<TaskUi?>(null) }

    // When gameId changes or screen loads, start fetching tasks for that game
    LaunchedEffect(gameId) {
        vm.start(gameId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF3ECE7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Show loading spinner while tasks are being loaded
            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Maroon)
                }
            } else if (state.error != null) {
                Text(
                    text = state.error ?: "Unknown error",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                if (state.isHost) {
                    HostTaskView(
                        state = state,
                        onApprove = { vm.approveSubmission(it) },
                        onReject = { vm.rejectSubmission(it) }
                    )

                } else {
                    PlayerTaskView(
                        state = state,
                        onTaskClick = { selectedTask = it }
                    )
                }
            }
        }
    }

    // Only show the dialog when a task has been selected
    selectedTask?.let { task ->
        TaskSubmissionDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onSubmit = { answer ->
                vm.submitTextAnswer(task.id, answer)
                selectedTask = null
            }
        )
    }
}

/**
 * Player View
 *
 */
@Composable
private fun PlayerTaskView(
    state: TaskUiState,
    onTaskClick: (TaskUi) -> Unit
) {
    Column {
        Text(
            text = "Tasks",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Team Score: ${state.teamScore} pts",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Maroon,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // how many tasks the player completed
        Text(
            text = "${state.completedCount}/${state.tasks.size} completed",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Show empty message if no tasks
        if (state.tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks available yet",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            // else show the list of tasks
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.tasks, key = { it.id }) { task ->
                    TaskCard(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

/**
 * Submission dialog
 *
 * This is the task card players see in the list
 */
@Composable
private fun TaskCard(
    task: TaskUi,
    onClick: () -> Unit
) {
    val bgColor = when {
        task.isCompleted -> Color(0xFFE8F5E9) // Green colour for completed
        task.isPending -> Color(0xFFFFF8E1)   // Yellow colour for pending review
        task.isRejected -> Color(0xFFFFEBEE)    // light red for rejected
        else -> Color.White
    }

    val isClickable = task.isRejected || (!task.isCompleted && !task.isPending)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = { if (isClickable) onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when {
                task.isCompleted -> Icons.Filled.CheckCircle
                task.isRejected -> Icons.Filled.Close
                task.isPending -> Icons.Filled.PanoramaFishEye
                else -> Icons.Filled.PanoramaFishEye
            }

            val iconTint = when {
                task.isCompleted -> Color(0xFF4CAF50)   // green
                task.isRejected -> Color.Red           // red X
                task.isPending -> Color(0xFFFFA000)    // yellow
                else -> Color.Gray
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )


            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
                if (task.isPending) {
                    Text(
                        text = "Awaiting approval...",
                        fontSize = 12.sp,
                        color = Color(0xFFFFA000),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${task.points} pts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Maroon
                )
                Text(
                    text = task.type.uppercase(),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
/**
 * Submission dialog
 */
// Just text for now
// TODO: Add UI and logic for photo submissions (camera/gallery) when task.type == "photo"
@Composable
private fun TaskSubmissionDialog(
    task: TaskUi,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var textAnswer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF3ECE7),
        shape = RoundedCornerShape(16.dp),
        title = {

            Text(
                text = task.name,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        },
        text = {
            Column {
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Text(
                    // TODO: Implement logic for point system
                    text = "Points: ${task.points}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Maroon,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = textAnswer,
                    onValueChange = { textAnswer = it },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {onSubmit(textAnswer)},
                enabled = textAnswer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Maroon)
            ) {
                Text("Submit", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Maroon)
            }
        }
    )
}
/**
 * Host View
 */
@Composable
private fun HostTaskView(
    state: TaskUiState,
    onApprove: (SubmissionUi) -> Unit,
    onReject: (SubmissionUi) -> Unit
) {
    Column {
        Text(
            text = "Submissions to Review",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "${state.pendingSubmissions.size} pending",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.pendingSubmissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No submissions to review",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.pendingSubmissions,key = { it.id }) { submission ->
                    SubmissionCard(
                        submission = submission,
                        onApprove = { onApprove(submission) },
                        onReject = { onReject(submission) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: SubmissionUi,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = submission.taskName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Text(
                    text = submission.type.uppercase(),
                    fontSize = 12.sp,
                    color = Maroon
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Team: ${submission.teamName}",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = "Submitted by: ${submission.submitterName}",
                fontSize = 13.sp,
                color = Color.Gray
            )
            if (!submission.textAnswer.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Answer:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = submission.textAnswer,
                            fontSize = 14.sp,
                            color = Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier =Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Maroon)
                ) {
                    Text("Approve", color = White)
                }
            }
        }
    }
}
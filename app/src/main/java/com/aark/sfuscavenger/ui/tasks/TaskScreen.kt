package com.aark.sfuscavenger.ui.tasks

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.ScavengerLoader
import com.aark.sfuscavenger.ui.theme.ScavengerDialog
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import java.io.File
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background


/**
 * TaskScreen handles both the player and host workflows for tasks
 * Players can view tasks, submit answers, and check their progress
 * Hosts can review submissions and approve/reject them
 */

@Composable
fun TaskScreen(
    gameId: String,
    vm: TaskViewModel = viewModel(),
    onEndGame: () -> Unit = {}
) {
    val context = LocalContext.current

    // Grab the latest UI state from the ViewModel
    // re-composes whenever state changes
    val state by vm.state.collectAsStateWithLifecycle()

    // Separate selection states for text vs photo tasks
    var selectedTextTask by remember { mutableStateOf<TaskUi?>(null) }
    var selectedPhotoTask by remember { mutableStateOf<TaskUi?>(null) }
    var showEndGameDialog by remember { mutableStateOf(false) }

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
                    ScavengerLoader()
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
                        onReject = { vm.rejectSubmission(it) },
                        onEndGame = { showEndGameDialog = true }
                    )

                } else {
                    PlayerTaskView(
                        state = state,
                        onTaskClick = { task ->
                            when (task.type) {
                                "photo" -> selectedPhotoTask = task
                                "text" -> selectedTextTask = task
                                else -> {
                                    selectedTextTask = task
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    selectedTextTask?.let { task ->
        TaskSubmissionDialog(
            task = task,
            onDismiss = { selectedTextTask = null },
            onSubmit = { answer ->
                vm.submitTextAnswer(task.id, answer, context)
                selectedTextTask = null
            }
        )
    }

    // Photo task dialog
    selectedPhotoTask?.let { task ->
        PhotoSubmissionDialog(
            task = task,
            onDismiss = { selectedPhotoTask = null },
            onSubmitPhoto = { bytes ->
                vm.submitPhotoAnswer(task.id, bytes, context)
                selectedPhotoTask = null
            }
        )
    }

    // End game confirmation dialog
    if (showEndGameDialog) {
        ScavengerDialog(
            onDismissRequest = { showEndGameDialog = false },
            title = "End Game?",
            text = {
                Text(
                    text = "Are you sure you want to end this game? This will finalize all scores and show the results.",
                    color = Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndGameDialog = false
                        vm.endGame()
                        onEndGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Maroon)
                ) {
                    Text("End Game", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndGameDialog = false }) {
                    Text("Cancel", color = Maroon)
                }
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
 * Task card in the player list
 */
@Composable
private fun TaskCard(
    task: TaskUi,
    onClick: () -> Unit
) {
    val bgColor = when {
        task.isCompleted -> Color(0xFFE8F5E9) // Green colour for completed
        task.isPending -> Color(0xFFFFF8E1)   // Yellow colour for pending review
        task.isRejected -> Color(0xFFFFEBEE)  // light red for rejected
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
 * Text Submission dialog
 */
@Composable
private fun TaskSubmissionDialog(
    task: TaskUi,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var textAnswer by remember { mutableStateOf("") }

    ScavengerDialog(
        onDismissRequest = onDismiss,
        title = task.name,
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
                onClick = { onSubmit(textAnswer) },
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

@Composable
private fun PhotoSubmissionDialog(
    task: TaskUi,
    onDismiss: () -> Unit,
    onSubmitPhoto: (ByteArray) -> Unit
) {
    val context = LocalContext.current

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            // Load bitmap for preview only
            val bmp = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(capturedImageUri!!)
            )
            previewBitmap = bmp
        }
    }

    fun launchCamera() {
        val file = File.createTempFile("photo_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        capturedImageUri = uri
        takePictureLauncher.launch(uri)
    }

    ScavengerDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = task.name,
        text = {
            Column {
                if (task.description.isNotBlank()) {
                    Text(task.description, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                }

                Text("Points: ${task.points}", color = Maroon)
                Spacer(Modifier.height(16.dp))

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("No photo captured yet", color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Maroon),
                    enabled = !isSubmitting
                ) {
                    Text(
                        if (previewBitmap == null) "Take Photo" else "Retake Photo",
                        color = White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (capturedImageUri != null) {
                        isSubmitting = true
                        val bytes = context.contentResolver
                            .openInputStream(capturedImageUri!!)!!
                            .readBytes()
                        onSubmitPhoto(bytes)
                    }
                },
                enabled = previewBitmap != null && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Maroon)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit", color = White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isSubmitting) onDismiss() },
                enabled = !isSubmitting
            ) {
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
    onReject: (SubmissionUi) -> Unit,
    onEndGame: () -> Unit
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.pendingSubmissions, key = { it.id }) { submission ->
                    SubmissionCard(
                        submission = submission,
                        onApprove = { onApprove(submission) },
                        onReject = { onReject(submission) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onEndGame,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            )
        ) {
            Text(
                text = "End Game",
                modifier = Modifier.padding(vertical = 8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: SubmissionUi,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {

    var showFullscreen by remember { mutableStateOf(false) }

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

            // Submitter name, team, and pfp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (!submission.submitterPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = submission.submitterPhotoUrl,
                        contentDescription = "Submitter Photo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(50)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFA46A4B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = submission.submitterName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submission.submitterName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    Text(
                        text = "Team: ${submission.teamName}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = submission.type.uppercase(),
                    fontSize = 12.sp,
                    color = Maroon
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = submission.taskName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Black
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

            // Photo preview (if photo submission)
            if (submission.type == "photo" && !submission.photoUrl.isNullOrBlank()) {
                var downloadUrl by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(submission.photoUrl) {
                    val ref = FirebaseStorage.getInstance()
                        .reference
                        .child(submission.photoUrl)
                    downloadUrl = ref.downloadUrl.await().toString()
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (downloadUrl != null) {
                    AsyncImage(
                        model = downloadUrl,
                        contentDescription = "Submitted photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                showFullscreen = true
                            },
                        contentScale = ContentScale.Crop
                    )

                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ScavengerLoader()
                        }
                    }

                if (showFullscreen && downloadUrl != null) {
                    Dialog(onDismissRequest = { showFullscreen = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = downloadUrl,
                                contentDescription = "Fullscreen photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
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
                    modifier = Modifier.weight(1f),
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
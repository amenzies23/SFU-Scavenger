package com.aark.sfuscavenger.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.aark.sfuscavenger.data.models.Submission
import com.aark.sfuscavenger.data.models.Task
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@Composable
fun MapScreen(
    gameId: String,
    vm: MapViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    // To start listening as soon as we have the ids
    LaunchedEffect(gameId) {
        vm.start(gameId)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState()

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Check/request permission
    // TODO: Move the permission request to happen on app startup
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Once we have permission, move camera to current location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            scope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    location?.let {
                        val here = LatLng(it.latitude, it.longitude)
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(here, 17f),
                            durationMs = 1000
                        )
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3ECE7))
            .padding(16.dp) ,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 8f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                    // Uncomment if using the hardcoded SFU location, and comment above ^
                    // isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true
                )
            ) {
                uiState.submissions.forEach { submission ->
                    val geo = submission.geo ?: return@forEach
                    val position = LatLng(geo.latitude, geo.longitude)
                    val hue = when (submission.status?.lowercase()) {
                        "approved" -> BitmapDescriptorFactory.HUE_GREEN
                        "pending" -> BitmapDescriptorFactory.HUE_ORANGE
                        "rejected" -> BitmapDescriptorFactory.HUE_RED
                        else -> BitmapDescriptorFactory.HUE_BLUE // fallback
                    }
                    key(submission.id) {
                        val markerState = rememberMarkerState(position = position)
                        Marker(
                            state = markerState,
                            title = "Task submission",
                            snippet = "Score: ${submission.scoreAwarded} pts",
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                vm.onMarkerSelected(submission)
                                true
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }

    // Loading indicator when UI is loading
    if (uiState.loading) {
        CircularProgressIndicator()
    }

    // Detail dialog when a pin is clicked
    uiState.selectedSubmission?.let { sub ->
        SubmissionDialog(
            submission = sub,
            task = uiState.selectedTask,
            loading = uiState.dialogLoading,
            onDismiss = { vm.onDialogDismiss() }
        )
    }
}
@Composable
private fun SubmissionDialog(
    submission: Submission,
    task: Task?,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF3ECE7),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF7B1F1F))
            }
        },
        title = { Text(text = "Task submission") },
        text = {
            Column {
                // Task info
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Loading task details…")
                    }
                } else {
                    Text(
                        text = "Name: ${task?.name ?: "Unknown task"}",
                        fontWeight = FontWeight.Bold
                    )
                    if (!task?.description.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = task!!.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Points: ${task?.points ?: submission.scoreAwarded}")
                }

                // Photo preview for photo tasks
                val isPhotoTask =
                    (task?.type == "photo") || (submission.type == "photo")

                if (isPhotoTask && submission.mediaStoragePath != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Photo:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    SubmissionPhotoPreview(
                        mediaStoragePath = submission.mediaStoragePath
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Submission info
                Text("Status: ${submission.status}")
                val submittedAt = submission.createdAt?.toDate()
                if (submittedAt != null) {
                    val fmt = remember {
                        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                    }
                    Text("Submitted at: ${fmt.format(submittedAt)}")
                }
            }
        }
    )
}

@Composable
private fun SubmissionPhotoPreview(
    mediaStoragePath: String
) {
    // Local state for loading the download URL
    var imageUrl by remember(mediaStoragePath) { mutableStateOf<String?>(null) }
    var loading by remember(mediaStoragePath) { mutableStateOf(true) }
    var error by remember(mediaStoragePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaStoragePath) {
        loading = true
        error = null
        try {
            val url = FirebaseStorage.getInstance()
                .reference
                .child(mediaStoragePath)
                .downloadUrl
                .await()
                .toString()

            imageUrl = url
        } catch (e: Exception) {
            error = "Failed to load image."
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
            error != null -> {
                Text(
                    text = error ?: "Error",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
            imageUrl != null -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Submitted photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

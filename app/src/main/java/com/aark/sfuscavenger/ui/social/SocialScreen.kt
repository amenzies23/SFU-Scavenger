package com.aark.sfuscavenger.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aark.sfuscavenger.data.models.Friend
import com.aark.sfuscavenger.ui.login.AuthViewModel
import com.aark.sfuscavenger.ui.theme.AppColors
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White

@Composable
fun SocialScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val profileState by viewModel.state.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    // based off of figma design
                    colorStops = arrayOf(
                        0.0f to Color(0xFFF3ECE7),  // Beige at top (0%)
                        0.66f to Color(0xFFD3C5BB), // Beige at 44%
                        1.0f to Color(0xFFD3C5BB)   // Light cream at bottom (100%)
                    )
                )
            )
    ) {
        // Social top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Social",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f) // bold
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Profile Settings",
                    tint = Color.Black
                )
            }
        }
        
        ProfileInformation(profileState)
        FriendsList(
            friends = profileState.friends,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        )

        if (showSettings) {
            ProfileSettingsDialog(
                currentState = profileState,
                onDismiss = { showSettings = false },
                onSave = { displayName, username, imageUri, removePhoto ->
                    viewModel.saveProfile(displayName, username, imageUri, removePhoto)
                },
                onLogout = {
                    authViewModel.signOut()
                    showSettings = false
                }
            )
        }
    }

}

@Composable
fun ProfileInformation(profileState: ProfileState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Determine background color for profile picture
        val profileBackgroundColor: Color
        if (profileState.profilePicture.isNullOrEmpty()) {
            profileBackgroundColor = AppColors.Red
        } else {
            profileBackgroundColor = Color.Transparent
        }
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(profileBackgroundColor)
        ) {
            if (!profileState.profilePicture.isNullOrEmpty()) {
                AsyncImage(
                    model = profileState.profilePicture,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Display Name
        val displayNameText: String
        if (profileState.displayName.isEmpty()) {
            displayNameText = "Display Name"
        } else {
            displayNameText = profileState.displayName
        }
        
        Text(
            text = displayNameText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        // Username
        val usernameText: String
        if (profileState.username.isEmpty()) {
            usernameText = "Username"
        } else {
            usernameText = profileState.username
        }
        
        Text(
            text = usernameText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Level Bar Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Level text on top
            Text(
                text = "Level ${profileState.userLevel}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Progress bar with XP text at bottom right
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Calculate progress based on XP
                val maxXP = 50 // TODO: Implement it to make it increase with level
                val progress = (profileState.totalXP.toFloat() / maxXP).coerceIn(0f, 1f)
                
                // Custom progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFEFAF4))
                        .border(
                            width = 2.dp,
                            color = Color(0xFF8A8079),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    // Filled portion based on progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxSize()
                            .background(Color(0xFFFEFAF4))
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                
                // XP text at bottom right
                Text(
                    text = "${profileState.totalXP} / 50xp",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(top = 22.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileSettingsDialog(
    currentState: ProfileState,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, imageUri: Uri?, removePhoto: Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var displayNameInput by rememberSaveable(currentState.displayName) {
        mutableStateOf(currentState.displayName)
    }
    var usernameInput by rememberSaveable(currentState.username) {
        mutableStateOf(currentState.username)
    }
    var localImageUri by remember(currentState.profilePicture) {
        mutableStateOf<Uri?>(null)
    }
    var removePhoto by rememberSaveable(currentState.profilePicture) {
        mutableStateOf(false)
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            localImageUri = uri
            removePhoto = false
        }
    }
    val previewImage = localImageUri?.toString()
        ?: if (!removePhoto) currentState.profilePicture else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        displayNameInput.trim(),
                        usernameInput.trim(),
                        localImageUri,
                        removePhoto
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,
                    contentColor = White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Maroon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log Out")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Red),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel")
                }
            }
        },
        title = { Text("Edit Profile") },
        containerColor = AlertDialogDefaults.containerColor.copy(alpha = 1f)
            .compositeOver(Color(0xFFFEFAF4)),
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Profile Photo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                if (previewImage.isNullOrEmpty()) AppColors.Red else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewImage.isNullOrEmpty()) {
                            Text(
                                text = "No Photo",
                                color = White,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            AsyncImage(
                                model = previewImage,
                                contentDescription = "Selected profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Maroon,
                                contentColor = White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Choose Photo")
                        }
                        OutlinedButton(
                            onClick = {
                                localImageUri = null
                                removePhoto = true
                            },
                            enabled = previewImage != null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Maroon),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove")
                        }
                    }
                }

                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

            }
        }
    )
}

@Composable
fun FriendsList(
    friends: List<Friend>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Friends",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (friends.isEmpty()) {
            Text(
                text = "Add friends to see them here.",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(30.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(friends, key = { it.id }) { friend ->
                    FriendRow(friend)
                }
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                color = Color(0xFFE1D5CD),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(friend)
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = friend.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = "Level ${friend.level}",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun FriendAvatar(friend: Friend) {
    val backgroundColor = if (friend.photoUrl.isNullOrEmpty()) AppColors.Red else Color.Transparent
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (friend.photoUrl.isNullOrEmpty()) {
            Text(
                text = friend.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            AsyncImage(
                model = friend.photoUrl,
                contentDescription = "${friend.displayName} profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

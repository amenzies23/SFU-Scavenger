package com.aark.sfuscavenger.ui.profile

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun ProfileScreen(
    navController: NavController,
    showSettings: Boolean,
    onRequestCloseSettings: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val profileState by viewModel.state.collectAsState()

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
        Spacer(modifier = Modifier.height(16.dp))
        
        ProfileInformation(profileState)
        FriendsList(
            friends = profileState.friends,
            addFriendError = profileState.addFriendError,
            onAddFriendByUsername = { username ->
                viewModel.addFriendByUsername(username)
            },
            onClearError = {
                viewModel.clearAddFriendError()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        )

        if (showSettings) {
            ProfileSettingsDialog(
                currentState = profileState,
                onDismiss = onRequestCloseSettings,
                onSave = { displayName, username, imageUri, removePhoto ->
                    viewModel.saveProfile(displayName, username, imageUri, removePhoto)
                },
                onLogout = {
                    authViewModel.signOut()
                    onRequestCloseSettings()
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
    val previewImage = when {
        localImageUri != null -> localImageUri.toString()
        removePhoto -> null
        else -> currentState.profilePicture
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    // Save profile (this will upload image to Firebase Storage)
                    onSave(
                        displayNameInput.trim(),
                        usernameInput.trim(),
                        localImageUri,
                        removePhoto
                    )
                    // Dismiss dialog after save is initiated
                    // The upload happens asynchronously in the background
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
        containerColor = Color(0xFFFEFAF4),
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
    addFriendError: String? = null,
    onAddFriendByUsername: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var isAddingFriend by remember { mutableStateOf(false) }
    
    // Clear error when dialog is opened
    LaunchedEffect(showAddFriendDialog) {
        if (showAddFriendDialog) {
            onClearError()
            usernameInput = ""
            isAddingFriend = false
        }
    }
    
    // Close dialog when friend is successfully added (no error after attempting to add)
    LaunchedEffect(addFriendError) {
        if (isAddingFriend && addFriendError == null) {
            // Friend was successfully added, close dialog and clear input
            usernameInput = ""
            showAddFriendDialog = false
            isAddingFriend = false
        }
    }
    
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Friends header with + button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Friends",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            IconButton(
                onClick = { showAddFriendDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Friend",
                    tint = Maroon
                )
            }
        }

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
    
    // Dialog to add friend by username
    if (showAddFriendDialog) {
        AddFriendDialogContent(
            usernameInput = usernameInput,
            addFriendError = addFriendError,
            isAddingFriend = isAddingFriend,
            onUsernameInputChange = { usernameInput = it },
            onAddFriend = {
                if (usernameInput.isNotBlank()) {
                    isAddingFriend = true
                    onAddFriendByUsername(usernameInput.trim())
                }
            },
            onDismiss = {
                showAddFriendDialog = false
                usernameInput = ""
            },
            onClearError = onClearError
        )
    }
}

@Composable
private fun AddFriendDialogContent(
    usernameInput: String,
    addFriendError: String?,
    isAddingFriend: Boolean,
    onUsernameInputChange: (String) -> Unit,
    onAddFriend: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Text(
                    "Add Friend by Username",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text field with red outline and beige background
                    TextField(
                        value = usernameInput,
                        onValueChange = { 
                            onUsernameInputChange(it)
                            // Clear error when user starts typing
                            if (addFriendError != null) {
                                onClearError()
                            }
                        },
                        label = { 
                            Text(
                                "Username",
                                color = if (addFriendError != null) AppColors.Red else Maroon,
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Enter username",
                                color = Color.Gray
                            ) 
                        },
                        singleLine = true,
                        isError = addFriendError != null,
                        shape = RoundedCornerShape(4.dp),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFEFAF4),
                            unfocusedContainerColor = Color(0xFFFEFAF4),
                            disabledContainerColor = Color(0xFFFEFAF4),
                            errorContainerColor = Color(0xFFFEFAF4),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedLabelColor = Maroon,
                            unfocusedLabelColor = Maroon,
                            errorLabelColor = AppColors.Red,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            errorTextColor = Color.Black,
                            cursorColor = Maroon
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (addFriendError != null) AppColors.Red else Maroon,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    // Show error message if there is one
                    if (addFriendError != null) {
                        Text(
                            text = addFriendError,
                            color = AppColors.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onAddFriend,
                    enabled = usernameInput.isNotBlank() && !isAddingFriend,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAddingFriend) {
                        Text("Adding")
                    } else { 
                        Text("Add Friend")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Maroon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFFFEFAF4),
            shape = RoundedCornerShape(28.dp)
        )
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

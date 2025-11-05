package com.aark.sfuscavenger.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aark.sfuscavenger.R
import com.aark.sfuscavenger.ui.theme.AppColors

@Composable
fun SocialScreen(navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Social top bar with settings 
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
            // https://fonts.google.com/icons?selected=Material+Symbols+Outlined:settings:FILL@0;wght@400;GRAD@0;opsz@24&icon.size=24&icon.color=%23000000
            Image(
                painter = painterResource(id = R.drawable.settings_logo),
                contentDescription = "Settings",
                modifier = Modifier.size(30.dp)
                // TODO: Implement pressing settings button flow
            )
        }
        
        ProfileInformation(viewModel)
    }
}

@Composable
fun SettingsDialog() {
    // TODO: Implement pop up settings later
}

@Composable
fun ProfileInformation(viewModel: ProfileViewModel) {
    val profileState = viewModel.state.collectAsState().value

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
            fontSize = 24.sp,
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Progress bar with XP text at bottom right
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Calculate progress based on XP (0-50)
                val maxXP = 50
                val progress = (profileState.totalXP.toFloat() / maxXP).coerceIn(0f, 1f)
                
                // Custom progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE0E0E0))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // Filled portion based on progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxSize()
                            .background(AppColors.Red)
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
fun FriendsList() {

}
package com.aark.sfuscavenger.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aark.sfuscavenger.ui.theme.Maroon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aark.sfuscavenger.repositories.GameRepository
import com.aark.sfuscavenger.repositories.TeamRepository
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex

data class PlacementUser(
    val id: String,
    val name: String,
    val score: Int,
    val photoUrl: String? = null,
    val placement: Int,
    val isTeam: Boolean = false
)

@Composable
fun PlacementScreen(
    navController: NavController,
    gameId: String,
    onNavigateHome: (() -> Unit)? = null
) {
    val gameRepository = remember { GameRepository() }
    val teamRepository = remember { TeamRepository() }
    
    var gameName by remember { mutableStateOf<String?>(null) }
    var top3 by remember { mutableStateOf<List<PlacementUser>>(emptyList()) }
    var others by remember { mutableStateOf<List<PlacementUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(gameId) {
        isLoading = true
        try {
            val game = gameRepository.getGame(gameId)
            gameName = game?.name
        } catch (e: Exception) {
            e.printStackTrace()
            gameName = "Unknown Game"
        }
        
        try {
            val teams = teamRepository.getTeams(gameId)
            
            val sortedTeams = teams.sortedByDescending { it.score }
            
            val uiPlacements = sortedTeams.mapIndexed { index, team ->
                PlacementUser(
                    id = team.id,
                    name = team.name,
                    score = team.score,
                    photoUrl = null,
                    placement = index + 1,
                    isTeam = team.memberCount > 1
                )
            }
            
            top3 = uiPlacements.filter { it.placement <= 3 }
            others = uiPlacements.filter { it.placement > 3 }
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Maroon)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3ECE7))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFECE0D8),
                                    Color(0xFFD0BCAE)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 20.dp + innerPadding.calculateTopPadding(), 
                                bottom = 0.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Results",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = gameName ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Maroon
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (top3.isNotEmpty()) {
                            PodiumSection(users = top3)
                        } else {
                            Spacer(modifier = Modifier.height(220.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(others) { user ->
                            LeaderboardItem(user = user)
                        }
                    }
                    
                    Button(
                        onClick = { 
                            if (onNavigateHome != null) {
                                onNavigateHome()
                            } else {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Maroon
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Back to Home",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumSection(users: List<PlacementUser>) {
    val first = users.find { it.placement == 1 }
    val second = users.find { it.placement == 2 }
    val third = users.find { it.placement == 3 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        if (third == null && second != null) {
            PodiumItem(
                user = second, 
                height = 180.dp, 
                color = Maroon.copy(alpha = 0.8f),
                modifier = Modifier.offset(x = 4.dp)
            )
            
            first?.let { 
                 PodiumItem(
                     user = it, 
                     height = 220.dp, 
                     color = Maroon, 
                     isFirst = true,
                     modifier = Modifier.zIndex(1f).offset(x = (-4).dp)
                 ) 
            }
        } else {
            second?.let { 
                 PodiumItem(
                     user = it, 
                     height = 180.dp, 
                     color = Maroon.copy(alpha = 0.8f),
                     modifier = Modifier.offset(x = 4.dp)
                 ) 
            }
            
            first?.let { 
                 PodiumItem(
                     user = it, 
                     height = 220.dp, 
                     color = Maroon, 
                     isFirst = true,
                     modifier = Modifier.zIndex(1f)
                 ) 
            }
            
            third?.let { 
                 PodiumItem(
                     user = it, 
                     height = 150.dp, 
                     color = Maroon.copy(alpha = 0.8f),
                     modifier = Modifier.offset(x = (-4).dp)
                 ) 
            }
        }
    }
}

@Composable
fun PodiumItem(
    user: PlacementUser,
    height: Dp,
    color: Color,
    isFirst: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rankColor = when (user.placement) {
        1 -> Color(0xFFD4AF37)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFFD4AF37)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        if (!user.isTeam) {
            Box(
                modifier = Modifier
                    .size(if (isFirst) 70.dp else 60.dp)
                    .offset(y = 10.dp)
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = user.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = if (isFirst) 3.dp else 0.dp,
                            color = if (isFirst) rankColor else Color.Transparent,
                            shape = CircleShape
                        )
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Spacer(modifier = Modifier.height(if (isFirst) 20.dp else 10.dp))
        }

        Text(
            text = user.name,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
                .zIndex(1f)
                .fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .width(if (isFirst) 100.dp else 100.dp)
                .height(height)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFA0303B),
                            Color(0xFFD49EA3)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF800000),
                    shape = androidx.compose.ui.graphics.RectangleShape
                )
        ) {
            if (user.isTeam) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.TopCenter)
                        .size(if (isFirst) 50.dp else 40.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.8f))
                        .border(
                             width = 2.dp,
                             color = Maroon,
                             shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.placement.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = if (isFirst) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge
                    )
                }
            }

            Text(
                text = "${user.score} pts",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (user.isTeam) 64.dp else (24.dp))
            )
        }
    }
}

@Composable
fun LeaderboardItem(user: PlacementUser) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 12.dp,
        color = Color(0xFFF3ECE7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Text(
                text = "${user.placement}.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${user.score} ${if (user.score == 1) "pt" else "pts"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Maroon
            )
        }
    }
}


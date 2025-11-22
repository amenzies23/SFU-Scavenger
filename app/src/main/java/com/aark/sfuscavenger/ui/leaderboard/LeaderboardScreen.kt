package com.aark.sfuscavenger.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aark.sfuscavenger.ui.theme.Black

// TODO: Replace this with the real leaderboard screen. Temporary for routing
@Composable
fun LeaderboardScreen(gameId: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3ECE7))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Leaderboard for game:\n$gameId\n\n(coming soon)",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
    }
}

package com.aark.sfuscavenger.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.Maroon

@Composable
fun LeaderboardScreen(
    gameId: String,
    vm: LeaderboardViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        vm.start(gameId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBeige)
            .padding(16.dp)
    ) {

        if (uiState.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading leaderboard…")
            }
            return@Column
        }

        uiState.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (uiState.rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No teams yet.\nJoin or create a team to appear here!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.rows, key = { it.rank }) { row ->
                    LeaderboardRow(row)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(row: LeaderboardRowUi) {
    val isMine = row.isMyTeam
    val bgColor = if (isMine) Maroon else Beige
    val textColor = if (isMine) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${row.rank}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textColor,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = row.teamName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = textColor
            )
            Text(
                text = "${row.score} pts",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }

        if (isMine) {
            Text(
                text = "Your team",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}

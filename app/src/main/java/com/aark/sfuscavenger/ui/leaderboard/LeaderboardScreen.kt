package com.aark.sfuscavenger.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.ErrorBanner
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.ScavengerBackgroundBrush

private val NeutralLeaderboardCardColor = Color(0xFFFFF4EC)

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
            .background(ScavengerBackgroundBrush)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Live standings for every team in this lobby.",
            style = MaterialTheme.typography.bodyMedium,
            color = Black.copy(alpha = 0.7f)
        )

        uiState.error?.let { message ->
            ErrorBanner(
                message = message,
                modifier = Modifier
                    .padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                uiState.loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Maroon)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading leaderboard…",
                            color = Black.copy(alpha = 0.8f)
                        )
                    }
                }

                uiState.rows.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No teams yet.\nJoin or create a team to appear here!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Black,
                            lineHeight = 22.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.rows, key = { it.rank }) { row ->
                            LeaderboardRow(row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(row: LeaderboardRowUi) {
    val isMine = row.isMyTeam
    val cardColor = if (isMine) Maroon else NeutralLeaderboardCardColor
    val textColor = if (isMine) Color.White else Color.Black
    val secondaryTextColor = textColor.copy(alpha = 0.8f)
    val badgeColor = if (isMine) Color.White.copy(alpha = 0.25f) else Maroon.copy(alpha = 0.1f)
    val badgeTextColor = Maroon

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankBadge(
                    rank = row.rank,
                    background = badgeColor,
                    textColor = badgeTextColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.teamName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                    Text(
                        text = "${row.score} pts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryTextColor
                    )
                }

                if (isMine) {
                    Text(
                        text = "Your team",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (row.members.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.members.forEach { member ->
                        Text(
                            text = "• $member",
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                }
            } else {
                Text(
                    text = "Waiting for players to join…",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = secondaryTextColor
                )
            }
        }
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    background: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

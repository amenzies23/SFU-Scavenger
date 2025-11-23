package com.aark.sfuscavenger.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aark.sfuscavenger.ui.theme.Maroon

@Composable
fun ResultsScreen(
    navController: NavController,
    gameId: String,
    teamId: String?
) {
    val background = Brush.verticalGradient(
        listOf(Color(0xFFF7F1EA), Color(0xFFF1E5DB))
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        color = Color.Transparent
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ResultsTopBar(
                    title = "Game summary",
                    onBack = { navController.popBackStack() }
                )
            }

            item {
                InfoCard {
                    Text(
                        text = "Game ID: $gameId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Team: ${teamId?.takeIf { it != "none" } ?: "Not assigned"}",
                        color = Maroon
                    )
                    Text(text = "Placement: 1st place")
                    Text(text = "Score: 320 pts")
                }
            }

            item { SectionTitle("Team members") }
            item {
                InfoCard {
                    Text(
                        text = "Team details will appear here once implemented.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { SectionTitle("Task progress") }
            item {
                InfoCard {
                    Text(
                        text = "Task progress will appear here once implemented.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { SectionTitle("Notes") }
            item {
                InfoCard {
                    Text(
                        text = "Additional notes will appear here once implemented.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = Color.White
                    )
                ) {
                    Text("Back to history")
                }
            }
        }
    }
}

@Composable
private fun ResultsTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFD7C3B5)), shape)
            .clip(shape),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = Color(0xFFFDF8F2)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Maroon
    )
}



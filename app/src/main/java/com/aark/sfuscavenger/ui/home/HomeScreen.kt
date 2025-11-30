package com.aark.sfuscavenger.ui.home

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aark.sfuscavenger.GameActivity

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeGame = vm.liveGamesForUser.firstOrNull()

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
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (activeGame != null) {
            Text(
                text = "Re-join active game session",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFE1D5CD)
                ),
                onClick = {
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putExtra("gameId", activeGame.id)
                    }
                    context.startActivity(intent)
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = activeGame.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
        } else {
            Text(
                text = "No active game session.",
                fontSize = 16.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

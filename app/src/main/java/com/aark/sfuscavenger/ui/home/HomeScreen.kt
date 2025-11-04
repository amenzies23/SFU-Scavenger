package com.aark.sfuscavenger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    val publicGames = viewModel.publicGames // empty

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3ECE7))
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Home",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Public Games box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD3C5BB))
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "Public Games",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Game Name",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Players",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

    }
}

package com.aark.sfuscavenger.ui.join

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun JoinScreen(navController: NavController) {
    var gameId by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Join Game", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = gameId,
            onValueChange = { gameId = it },
            label = { Text("Enter Game ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (gameId.isNotBlank()) {
                    navController.navigate("lobby/$gameId")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join Game")
        }
    }
}

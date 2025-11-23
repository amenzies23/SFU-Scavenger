package com.aark.sfuscavenger.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.Maroon

@Composable
fun TaskScreen(
    gameId: String,
    vm: TaskViewModel = viewModel()
) {
    val state = vm.state.collectAsState().value

    // When we enter the screen, load the tasks for this game
    LaunchedEffect(gameId) {
        vm.loadTasks(gameId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF3ECE7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // header text
            Text(
                text = "Tasks",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Maroon)
                    }
                }

                state.error != null -> {
                    Text(
                        text = state.error ?: "Error",
                        color = Color.Red,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                else -> {
                    // Basic list of tasks
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.tasks) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {

                                        // Task name
                                        Text(
                                            text = task.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )

                                        // Description
                                        if (task.description.isNotBlank()) {
                                            Text(
                                                text = task.description,
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        // Points
                                        Text(
                                            text = "${task.points} points",
                                            fontSize = 13.sp,
                                            color = Maroon
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

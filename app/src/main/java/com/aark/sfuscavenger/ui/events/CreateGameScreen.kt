package com.aark.sfuscavenger.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.DarkOrange
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White

@Composable
fun CreateGameScreen(navController: NavController, gameId: String? = null, vm: CreateGameViewModel = viewModel()) {
    val selectedTab = remember { mutableStateOf(0) }
    val gameCreated = vm.gameCreated.collectAsState()

    LaunchedEffect(gameId) {
        if (gameId != null) {
            vm.loadGame(gameId)
        }
    }

    LaunchedEffect(gameCreated.value) {
        if (gameCreated.value) {
            navController.popBackStack()
        }
    }

    CreateGameScreenContent(
        selectedTabIndex = selectedTab.value,
        onTabSelected = { selectedTab.value = it },
        navController = navController,
        modifier = Modifier.background(Beige),
        vm = vm,
        isEditMode = gameId != null
    )
}

@Composable
private fun CreateGameScreenContent(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: CreateGameViewModel,
    isEditMode: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxSize()
    ){
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = LightBeige,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Maroon
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Game", color = Black)}
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Tasks", color = Black)}
            )
        }

        when (selectedTabIndex) {
            0 -> GameTab(navController = navController, vm, isEditMode = isEditMode, modifier = Modifier.fillMaxSize().padding(16.dp))
            1 -> TasksTab(navController = navController, vm, modifier = Modifier.fillMaxSize().padding(16.dp))
        }
    }
}

@Composable
private fun GameTab(navController: NavController,
                    vm: CreateGameViewModel,
                    isEditMode: Boolean = false,
                    modifier: Modifier = Modifier
) {
    val game = vm.game.collectAsState()
    val loading = vm.loading.collectAsState()
    val error = vm.error.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Game Name
                TextField(
                    value = game.value.name,
                    onValueChange = { vm.updateName(it) },
                    label = { Text("Game Name", color = Maroon, fontWeight = Bold) },
                    placeholder = { Text("Enter game name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = LightBeige,
                        unfocusedContainerColor = LightBeige,
                    ),
                    singleLine = true
                )
            }

            item {
                // Description
                TextField(
                    value = game.value.description,
                    onValueChange = { vm.updateDescription(it) },
                    label = { Text("Description", color = Maroon, fontWeight = Bold) },
                    placeholder = { Text("Enter game description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = LightBeige,
                        unfocusedContainerColor = LightBeige,
                    ),
                    maxLines = 5
                )
            }

            item {
                // Join Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Private Game (Join by Code)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black
                    )

                    Switch(
                        checked = game.value.joinMode == "code",
                        onCheckedChange = { isChecked ->
                            vm.updateJoinMode(if (isChecked) "code" else "open")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = Maroon,
                            uncheckedThumbColor = Maroon,
                            uncheckedTrackColor = LightBeige
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            if (error.value != null) {
                item {
                    Text(
                        text = error.value ?: "",
                        color = DarkOrange,
                        fontSize = 14.sp
                    )
                }
            }

        }

        // Save Button
        Button(
            onClick = { vm.saveGame() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            ),
            enabled = !loading.value && game.value.name.isNotBlank()
        ) {
            Text(
                text = when {
                    loading.value -> "Saving..."
                    isEditMode -> "Update"
                    else -> "Save"
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightBeige,
                contentColor = Black
            ),
            enabled = !loading.value
        ){
            Text(
                text = "Cancel",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

    }

}

@Composable
private fun TasksTab(navController: NavController,
                    vm: CreateGameViewModel,
                    modifier: Modifier = Modifier
) {
    Text(text = "Task Tab Content")
}
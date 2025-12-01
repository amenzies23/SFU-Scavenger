package com.aark.sfuscavenger.ui.events

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.data.models.Game
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.SFUScavengerTheme
import com.aark.sfuscavenger.ui.theme.White
import com.aark.sfuscavenger.ui.theme.DarkOrange
import com.aark.sfuscavenger.ui.theme.ScavengerBackgroundBrush
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(navController: NavController, vm: EventsViewModel = viewModel()) {
    val selectedTab = remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScavengerBackgroundBrush)
    ) {
        EventsContent(
            selectedTabIndex = selectedTab.intValue,
            onTabSelected = { selectedTab.intValue = it },
            navController = navController,
            modifier = Modifier.fillMaxSize(),
            vm = vm
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsContent(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: EventsViewModel
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex,
                        matchContentSize = false
                    ),
                    width = 64.dp,
                    color = Maroon
                )
            },
            divider = {} 
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Join", color = Black) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Create", color = Black) }
            )
        }

        when (selectedTabIndex) {
            0 -> JoinTab(navController = navController, vm = vm, modifier = Modifier.fillMaxSize())
            1 -> CreateTab(navController = navController, vm = vm,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}


@Composable
private fun JoinTab(navController: NavController,
                    vm: EventsViewModel,
                    modifier: Modifier = Modifier
) {
    val publicGames = vm.publicGames.collectAsState()
    val privateGames = vm.privateGames.collectAsState()
//    val loading = vm.loading.collectAsState()

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
    ){

        Text(
            text = "Public Games",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DarkOrange,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )

        // Container for list with scrollbar
        Box(
             modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
    
                items(publicGames.value) { game ->
                    GameRow(
                        game = game,
                        onJoinClick = {
                            navController.navigate("lobby/${game.id}")
                        }
                    )
                }
            }
        }

        // Spacer(modifier = Modifier.height(8.dp))
        // Orange bar on the top of join by code
        HorizontalDivider(
            color = DarkOrange,
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Join By Code",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DarkOrange,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            JoinByCodeRow(navController = navController, privateGames = privateGames.value)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

}

@Composable
private fun CreateTab(navController: NavController,
                      vm: EventsViewModel,
                      modifier: Modifier = Modifier
) {
    val myGames = vm.myGames.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            text = "My Games",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DarkOrange
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {

            items(myGames.value) { game ->
                MyGamesRow(
                    game = game,
                    onLaunchClick = {
                        Log.d("MyGamesRow", "Launch clicked for game: ${game.name}")
                        vm.publishGame(game.id)
                        navController.navigate("lobby/${game.id}")

                    },
                    onEditClick = {
                        Log.d("MyGamesRow", "Edit clicked for game: ${game.name}")
                        navController.navigate("createGame?gameId=${game.id}")
                    },
                    onDeleteClick = {
                        Log.d("MyGamesRow", "Delete clicked for game: ${game.name}")
                        vm.deleteGame(game.id)
                    }
                )
            }

        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("createGame") },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Game +")
        }


    }
}

@Composable
private fun MyGamesRow(
    game: Game,
    onEditClick: () -> Unit,
    onLaunchClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { isExpanded = !isExpanded }
            .border(
                width = 1.dp,
                color = Color(0xFFE1D5CD),
                shape = shape
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.name,
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLaunchClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Maroon,
                            contentColor = White
                        )
                    ) {
                        Text("Launch")
                    }

                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Maroon,
                            contentColor = White
                        )
                    ) {
                        Text("Edit")
                    }

                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkOrange,
                            contentColor = White
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun GameRow(
    game: Game,
    onJoinClick: () -> Unit,
){
    Log.d("GameRow", "Displaying row for: ${game.name}")
    var isExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { isExpanded = !isExpanded }
            .border(
                width = 1.dp,
                color = Color(0xFFE1D5CD),
                shape = shape
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    text = game.name,
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onJoinClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = White
                    )
                ) {
                    Text("Join")
                }
            }

            if (isExpanded && game.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = game.description,
                    color = DarkOrange,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun JoinByCodeRow(
    navController: NavController,
    privateGames: List<Game>,
) {
    var code by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .background(
                color = LightBeige,
                shape = RoundedCornerShape(16.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TextField(
            value = code,
            onValueChange = { code = it },
            placeholder = { Text("Enter Join code") },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = White,
                unfocusedContainerColor = White,
            ),
            modifier = Modifier
                .weight(1f)
                .background(
                    color = LightBeige,
                    shape = RoundedCornerShape(16.dp)
                ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        Button(
            onClick = {
                val match = privateGames.firstOrNull { it.joinCode == code.trim() }
                if (match != null && match.status == "live") {
                    navController.navigate("lobby/${match.id}")
                } else {
                    // Handle invalid code (e.g., show a message to the user)
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            )
        ) {
            Text("Join")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun JoinScreenPreview() {
    val nav = rememberNavController()
    SFUScavengerTheme {
        EventsScreen(navController = nav)
    }
}

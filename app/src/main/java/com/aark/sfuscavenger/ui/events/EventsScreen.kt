package com.aark.sfuscavenger.ui.events

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.data.models.Game
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.aark.sfuscavenger.ui.lobby.LobbyScreen
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.SFUScavengerTheme
import com.aark.sfuscavenger.ui.theme.White
import com.aark.sfuscavenger.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(navController: NavController, vm: EventsViewModel = viewModel()) {
    val selectedTab = remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = rememberTopAppBarState()
    )

    Scaffold(
        topBar = {
            TopBar(title = "Events")
        },
        containerColor = Beige
    ) { paddingValues ->
        EventsContent(
            selectedTabIndex = selectedTab.intValue,
            onTabSelected = { selectedTab.intValue = it },
            scrollBehavior = scrollBehavior,
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            vm = vm
        )
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun TopBar(
//    modifier: Modifier = Modifier,
//
//) {
//    Column {
//        TopAppBar(
//            modifier = modifier,
//            colors = TopAppBarDefaults.topAppBarColors(
//                containerColor = Maroon
//            ),
//            title = {
//                Text(
//                    text = "Events",
//                    color = White
//                )
//            }
//        )
//    }
//
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsContent(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: EventsViewModel

) {
    Column(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize()
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
                text = { Text("Join", color = Black)}
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Create", color = Black)}
            )
        }

        when (selectedTabIndex) {
            0 -> JoinTab(navController = navController, vm, modifier = Modifier.fillMaxSize().padding(16.dp))
            1 -> CreateTab(navController = navController, modifier = Modifier.fillMaxSize().padding(16.dp))
        }
    }



}

@Composable
private fun JoinTab(navController: NavController,
                    vm: EventsViewModel,
                    modifier: Modifier = Modifier
) {
    val games = vm.games.collectAsState()
//    val loading = vm.loading.collectAsState()
    val error = vm.error.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadGames()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
//            .padding(16.dp)
    ){
        LazyColumn {
            items(games.value) { game ->
                GameRow(
                    game = game,
                    onJoinClick = {
                        navController.navigate("lobby/${game.id}")
                    }
                )
            }
        }
//        when {
//            loading.value -> {
////                Text("Loading: ")
//                Text("")
//            }
//
//            error.value != null -> {
//                Text("Error: ${error.value}")
//            }
//
//            else -> {
//                LazyColumn {
//                    items(games.value) { game ->
//                        GameRow(
//                            game = game,
//                            onJoinClick = {
//                                navController.navigate("lobby/${game.id}")
//                            }
//                        )
//                    }
//                }
//            }
//        }
    }

}

@Composable
private fun CreateTab(navController: NavController, modifier: Modifier = Modifier) {
    Text("Create Tab", modifier = modifier.padding(16.dp))
}

@Composable
fun GameRow(
    game: Game,
    onJoinClick: () -> Unit,
){
    Log.d("GameRow", "Displaying row for: ${game.name}")

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
        Text(
            text = game.name,
            color = Black,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onJoinClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            ),
            modifier = Modifier
                .padding(end = 4.dp)

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

package com.aark.sfuscavenger.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.SFUScavengerTheme
import com.aark.sfuscavenger.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(navController: NavController) {
    val selectedTab = remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = rememberTopAppBarState()
    )

    Scaffold(
        topBar = {
            TopBar(
                selectedTab = selectedTab.intValue,
                onTabSelected = { selectedTab.intValue = it},
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when (selectedTab.intValue) {
            0 -> JoinTab(navController, modifier = Modifier.padding(paddingValues))
            1 -> CreateTab(navController = navController, modifier = Modifier.padding(paddingValues))
        }
    }
}


//@Composable
//private fun JoinTab(navController: NavController) {
//    Text("Join Tab")
//}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun TopBar(
//    selectedTab: Int,
//    onTabSelected: (Int) -> Unit,
//    modifier: Modifier = Modifier,
//    scrollBehavior: TopAppBarScrollBehavior,
//) {
//    TopAppBar(
//        modifier = modifier,
//        scrollBehavior = scrollBehavior,
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = MaterialTheme.colorScheme.primary
//        ),
//        title = {
//        },
//        actions = {
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                TextButton(onClick = { onTabSelected(0) }) {
//                    Text(
//                        text = "Join",
//                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary
//                        else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
//                    )
//                }
//                TextButton(onClick = { onTabSelected(1) }) {
//                    Text(
//                        text = "Create",
//                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary
//                        else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
//                    )
//                }
//            }
//        }
//    )
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    Column {
        TopAppBar(
            modifier = modifier,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Maroon
            ),
            title = {
                Text(
                    text = "Events",
                    color = White
                )
            }
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Beige,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Maroon
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Join", color = Black)}
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Create", color = Black)}
            )
        }
    }

}

@Composable
private fun JoinTab(navController: NavController, modifier: Modifier = Modifier) {
    Text("Join Tab", modifier = modifier.padding(16.dp))
}

@Composable
private fun CreateTab(navController: NavController, modifier: Modifier = Modifier) {
    Text("Create Tab", modifier = modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
fun JoinScreenPreview() {
    val nav = rememberNavController()
    SFUScavengerTheme {
        EventsScreen(navController = nav)
    }
}


//fun JoinScreenPreview() {
//    val nav = rememberNavController()
//    SFUScavengerTheme {
//        SignInScreen(
//            navController = nav,
//            loading = false,
//            error = null,
//            onLogin = { _, _ -> },
//            onGoToSignUp = {}
//        )
//    }
//}
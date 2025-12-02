package com.aark.sfuscavenger.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aark.sfuscavenger.data.models.Game
import com.aark.sfuscavenger.ui.map.SharedMap
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel
) {
    val mapGames = vm.mapGames
    var selectedGameForJoin by remember { mutableStateOf<Game?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3ECE7))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Games Map",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HomeGamesMap(
            games = mapGames,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onGameMarkerClick = { game ->
                selectedGameForJoin = game
            }
        )
    }

    selectedGameForJoin?.let { game ->
        HomeGameJoinDialog(
            game = game,
            onDismiss = { selectedGameForJoin = null },
            onJoin = {
                selectedGameForJoin = null
                navController.navigate("lobby/${game.id}")
            }
        )
    }
}

@Composable
private fun HomeGamesMap(
    games: List<Game>,
    modifier: Modifier = Modifier,
    onGameMarkerClick: (Game) -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
            .border(
                width = 3.dp,
                color = Beige.copy(alpha = 0.9f),
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Beige.copy(alpha = 0.2f))
    ) {
        SharedMap {
            games.forEach { game ->
                val loc = game.location ?: return@forEach

                val hue = when {
                    game.status == "live" -> BitmapDescriptorFactory.HUE_BLUE
                    game.status == "draft" && game.startTime != null ->
                        BitmapDescriptorFactory.HUE_ORANGE
                    else -> return@forEach
                }

                val position = LatLng(loc.latitude, loc.longitude)

                Marker(
                    state = rememberMarkerState(position = position),
                    title = game.name,
                    snippet = when {
                        game.status == "live" -> "Live game"
                        else -> "Scheduled game"
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(hue),
                    onClick = {
                        onGameMarkerClick(game)
                        true
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeGameJoinDialog(
    game: Game,
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    val isLiveJoinable =
        game.status == "live" && game.joinMode == "open"

    val isScheduled =
        game.status == "draft" && game.startTime != null

    val formatter = remember {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    }

    val startTimeText = remember(game.startTime) {
        game.startTime?.toDate()?.let { date ->
            formatter.format(date)
        }
    }

    val joinableFromText = remember(game.startTime) {
        if (!isScheduled) null
        else {
            game.startTime?.toDate()?.let { start ->
                val joinMillis = start.time - 30L * 60L * 1000L
                formatter.format(java.util.Date(joinMillis))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LightBeige,
        title = {
            Text(
                text = game.name,
                fontWeight = FontWeight.Bold,
                color = Black
            )
        },
        text = {
            Column {
                if (!game.description.isNullOrBlank()) {
                    Text(text = game.description!!, color = Black)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(text = "Status: ${game.status}", color = Black)
                Text(text = "Join mode: ${game.joinMode}", color = Black)

                if (startTimeText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Start time: $startTimeText", color = Black)
                }

                if (isScheduled && joinableFromText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Joinable from: $joinableFromText\n(30 minutes before start)",
                        color = Black,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isLiveJoinable ->
                            "This game is live and open. Join to enter the lobby."
                        else ->
                            "This game is not currently joinable."
                    },
                    fontSize = 14.sp,
                    color = Black
                )
            }
        },
        confirmButton = {
            if (isLiveJoinable) {
                Button(
                    onClick = onJoin,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = White
                    )
                ) {
                    Text("Join game")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,
                        contentColor = White
                    )
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (isLiveJoinable) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightBeige,
                        contentColor = Black
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
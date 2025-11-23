package com.aark.sfuscavenger.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aark.sfuscavenger.ui.theme.Maroon

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val background = Brush.verticalGradient(
        listOf(Color(0xFFF7F1EA), Color(0xFFF1E5DB))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryHeader()

            when {
                uiState.loading -> HistoryLoading()
                uiState.error != null -> HistoryError(message = uiState.error ?: "")
                uiState.cards.isEmpty() -> HistoryEmpty(onRefresh = { viewModel.refreshHistory() })
                else -> {
                    HistoryList(
                        modifier = Modifier.weight(1f),
                        cards = uiState.cards,
                        onCardSelected = { card ->
                            val encodedGameId = Uri.encode(card.gameId)
                            val encodedTeamId = Uri.encode(card.teamId ?: "none")
                            navController.navigate("results/$encodedGameId/$encodedTeamId")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search history"
                )
            }
        }
        Text(
            text = "Previous Games",
            color = Maroon,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HistoryLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        CircularProgressIndicator(color = Maroon)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading history…")
    }
}

@Composable
private fun HistoryError(
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun HistoryEmpty(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You haven’t joined any games yet.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = Color.White
            )
        ) {
            Text("Refresh")
        }
    }
}

@Composable
private fun HistoryList(
    modifier: Modifier = Modifier,
    cards: List<HistoryCard>,
    onCardSelected: (HistoryCard) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(cards, key = { it.gameId }) { card ->
            HistoryCardItem(card = card, onClick = { onCardSelected(card) })
        }
    }
}

@Composable
private fun HistoryCardItem(
    card: HistoryCard,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = shape, clip = false)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        color = Color(0xFFFDF8F2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = card.placement,
                color = Maroon,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

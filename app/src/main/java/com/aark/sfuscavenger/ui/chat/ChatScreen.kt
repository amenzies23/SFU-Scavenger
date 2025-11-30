package com.aark.sfuscavenger.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextFieldDefaults
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.ScavengerLoader
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White

@Composable
fun ChatScreen(
    gameId: String,
    vm: ChatViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(gameId) {
        vm.start(gameId)
    }
    LaunchedEffect(uiState.messages.size) {
        val lastIndex = uiState.messages.lastIndex
        if (lastIndex >= 0) {
            listState.scrollToItem(lastIndex)
            vm.markAllMessagesRead()
        }
    }

    // TODO: Fix chat UI issue when keyboard opens, it pushes the whole bottom navbar up
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3ECE7))
            .imePadding()
            .padding(16.dp)
    ) {
        uiState.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(8.dp)
        ) {
            if (uiState.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ScavengerLoader()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    items(uiState.messages) { msg ->
                        ChatMessageBubble(msg = msg)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = uiState.inputText,
                onValueChange = vm::onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = {
                    Text(
                        "Type a message…",
                        color = Color(0xFF7B1F1F).copy(alpha = 0.4f)
                    )
                },
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Beige,
                    unfocusedContainerColor = Beige,
                    disabledContainerColor = Beige,
                    cursorColor = Maroon,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { vm.sendCurrentMessage() },
                enabled = uiState.inputText.isNotBlank(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,
                    contentColor = Color.White,
                    disabledContainerColor = Beige,
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(
                    horizontal = 20.dp,
                    vertical = 10.dp
                )
            ) {
                Text(
                    text = "Send",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(msg: ChatMessageUi) {
    val bubbleColor = if (msg.isMine) Maroon else Beige
    val textColor = if (msg.isMine) Color.White else Color.Black

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(10.dp)
        ) {
            // Top row to display name + level + time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append(msg.senderName)
                        append(" (Lv.")
                        append(msg.senderLevel)
                        append(")")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.9f)
                )
                if (msg.time.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = msg.time,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message body
            Text(
                text = msg.text,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

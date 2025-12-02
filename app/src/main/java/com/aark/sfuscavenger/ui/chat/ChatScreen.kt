package com.aark.sfuscavenger.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.ScavengerLoader
import com.aark.sfuscavenger.ui.theme.Maroon
import kotlin.math.roundToInt

@Composable
fun ChatScreen(
    gameId: String,
    vm: ChatViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // swipe to reveal timestamps state
    val maxRevealDp = 80.dp
    val density = LocalDensity.current
    val maxRevealPx = with(density) { maxRevealDp.toPx() }

    var dragOffsetPx by remember { mutableStateOf(0f) }
    val animatedOffsetPx by animateFloatAsState(
        targetValue = dragOffsetPx,
        label = "timeRevealOffset"
    )
    val timeRevealProgress = (animatedOffsetPx / maxRevealPx).coerceIn(0f, 1f)

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
    val chatEnabled = uiState.teamId != null
    val showJoinTeamOverlay = !chatEnabled && !uiState.loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBeige)
            .imePadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        Text(
            text = "Team Chat",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Black,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = "Coordinate with your teammates and keep everyone in sync.",
            style = MaterialTheme.typography.bodySmall.copy(color = Maroon.copy(alpha = 0.7f)),
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .border(BorderStroke(2.dp, Beige), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (chatEnabled) 1f else 0.35f)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                if (uiState.loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ScavengerLoader()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount ->
                                        // dragAmount < 0 when swiping left
                                        val newValue = (dragOffsetPx + -dragAmount)
                                            .coerceIn(0f, maxRevealPx)
                                        dragOffsetPx = newValue
                                    },
                                    onDragEnd = { dragOffsetPx = 0f },
                                    onDragCancel = { dragOffsetPx = 0f }
                                )
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState
                    ) {
                        items(uiState.messages) { msg ->
                            ChatMessageBubble(
                                msg = msg,
                                timeRevealProgress = timeRevealProgress
                            )
                        }
                    }
                }
            }
            }

            if (showJoinTeamOverlay) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Maroon.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Join a team to have a chat!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chat unlocks once you’re part of a team.",
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (chatEnabled) 1f else 0.35f)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        BorderStroke(2.dp, Beige),
                        RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.9f))
            ) {
                TextField(
                    value = uiState.inputText,
                    onValueChange = vm::onInputChanged,
                    enabled = chatEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Type a message…",
                            color = Maroon.copy(alpha = 0.45f)
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = Maroon,
                        fontSize = 14.sp
                    ),
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Maroon,
                        unfocusedTextColor = Maroon,
                        disabledTextColor = Maroon.copy(alpha = 0.4f),
                        focusedPlaceholderColor = Maroon.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = Maroon.copy(alpha = 0.4f),
                        disabledPlaceholderColor = Maroon.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = Maroon,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { vm.sendCurrentMessage() },
                enabled = chatEnabled && uiState.inputText.isNotBlank(),
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
}

@Composable
private fun ChatMessageBubble(
    msg: ChatMessageUi,
    timeRevealProgress: Float
) {
    val bubbleColor = if (msg.isMine) Maroon else Beige
    val textColor = if (msg.isMine) Color.White else Color.Black
    val bubbleShape = if (msg.isMine) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp
        )
    }
    val bubbleBorderColor = if (msg.isMine) {
        Color.White.copy(alpha = 0.6f)
    } else {
        Maroon.copy(alpha = 0.3f)
    }

    val density = LocalDensity.current
    val timeColumnBase = 60.dp
    val maxShiftDp = timeColumnBase / 2
    val shiftPx = with(density) { (maxShiftDp * timeRevealProgress).toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar only for others
        if (!msg.isMine) {
            ChatAvatar(
                photoUrl = msg.senderPhotoUrl,
                size = 28.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Bubble area takes remaining width minus time column
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = if (msg.isMine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            // Only my messages slide left on reveal (locking the left ones in place)
            val bubbleShiftPx = if (msg.isMine) shiftPx else 0f

            Column(
                modifier = Modifier
                    .offset { IntOffset(-bubbleShiftPx.roundToInt(), 0) }
                    .widthIn(max = 260.dp)
                    .shadow(2.dp, bubbleShape)
                    .clip(bubbleShape)
                    .border(BorderStroke(1.dp, bubbleBorderColor), bubbleShape)
                    .background(bubbleColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (!msg.isMine) {
                    Text(
                        text = "${msg.senderName} (Lv.${msg.senderLevel})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Maroon.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = msg.text,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
        }

        Box(
            modifier = Modifier.width(timeColumnBase * timeRevealProgress),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (msg.time.isNotEmpty()) {
                // Only start drawing text once theres enough space to avoid vertical wrapping
                val visibleStart = 0.7f
                val rawAlpha = ((timeRevealProgress - visibleStart) / (1f - visibleStart))
                val timeAlpha = rawAlpha.coerceIn(0f, 1f)

                if (timeAlpha > 0f) {
                    Text(
                        text = msg.time,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.alpha(timeAlpha)
                    )
                }
            }
        }

    }
}


@Composable
private fun ChatAvatar(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "User profile photo",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Red)
        )
    }
}

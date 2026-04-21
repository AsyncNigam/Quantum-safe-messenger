package com.nigdroid.quantummessenger.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.ChatUiState
import com.nigdroid.quantummessenger.presentation.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main chat screen composable with glassmorphism design.
 *
 * Features:
 * - Edge-to-edge layout under status and navigation bars
 * - Animated background with mesh gradient
 * - Glassmorphism message bubbles
 * - Real-time message updates via ViewModel
 * - Smooth animations for message entry
 * - Responsive input field with send button
 * - Graceful error and loading states
 */
@Composable
fun ChatScreen(
    currentUserId: String = "current_user",
    participantId: String = "participant_user",
    viewModel: ChatViewModel = hiltViewModel()
) {
    var messageInput by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()

    // Initialize ViewModel on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize(currentUserId, participantId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Success) {
            val messageCount = (uiState as ChatUiState.Success).messages.size
            if (messageCount > 0) {
                lazyListState.animateScrollToItem(messageCount - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Animated background
        AnimatedMeshGradientBackground(
            modifier = Modifier.fillMaxSize()
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Header
            ChatHeader()

            // Content based on state
            when (uiState) {
                is ChatUiState.Loading -> {
                    LoadingState()
                }

                is ChatUiState.Success -> {
                    val state = uiState as ChatUiState.Success
                    ChatMessageList(
                        messages = state.messages,
                        currentUserId = currentUserId,
                        lazyListState = lazyListState,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ChatInputField(
                        messageInput = messageInput,
                        onMessageChange = { messageInput = it },
                        onSendClick = {
                            if (messageInput.isNotBlank()) {
                                viewModel.sendMessage(messageInput)
                                messageInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        isSending = state.isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                is ChatUiState.Error -> {
                    val state = uiState as ChatUiState.Error
                    ErrorState(
                        errorMessage = state.message,
                        onRetry = { viewModel.retryLoadingMessages() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Chat screen header with title and participant info.
 */
@Composable
private fun ChatHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AFFFFFF))
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "Secure Chat",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = "End-to-end encrypted",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Loading state UI with animated progress indicator.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading messages...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Error state UI with retry option.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Oops!",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Button(
                onClick = onRetry,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}

/**
 * LazyColumn displaying chat messages with glassmorphism bubbles.
 */
@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    currentUserId: String,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        state = lazyListState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = slideOutVertically()
            ) {
                MessageBubble(
                    message = message,
                    isOwn = message.senderId == currentUserId
                )
            }
        }
    }
}

/**
 * Individual message bubble with glassmorphism effect.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isOwn: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .glassmorphism(
                    blurRadius = 15f,
                    cornerRadius = 16,
                    isDarkTheme = false
                )
                .background(
                    color = if (isOwn) {
                        Color(0x4D6366FF) // Blue tint for own messages
                    } else {
                        Color(0x4DFFFFFF) // White tint for others
                    }
                )
                .padding(12.dp),
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.content,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatTimestamp(message.timestamp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Glassmorphic chat input field with send button.
 */
@Composable
private fun ChatInputField(
    messageInput: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .glassmorphism(
                blurRadius = 20f,
                cornerRadius = 24,
                isDarkTheme = false
            )
            .background(Color(0x4DFFFFFF))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextField(
            value = messageInput,
            onValueChange = onMessageChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            placeholder = { Text("Type a message...", fontSize = 14.sp) },
            maxLines = 3,
            singleLine = false,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (messageInput.isNotBlank() && !isSending) {
                        onSendClick()
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            enabled = !isSending,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        IconButton(
            onClick = onSendClick,
            enabled = messageInput.isNotBlank() && !isSending,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                .padding(8.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = Color.White,
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp)
                )
            }
        }
    }
}

/**
 * Formats timestamp to HH:mm format.
 */
private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

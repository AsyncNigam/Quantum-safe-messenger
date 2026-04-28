package com.nigdroid.quantummessenger.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nigdroid.quantummessenger.domain.model.ChatMessage
import com.nigdroid.quantummessenger.domain.model.MessageStatus
import com.nigdroid.quantummessenger.domain.model.MessageType
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphismBubbleOwn
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphismBubbleOther
import com.nigdroid.quantummessenger.presentation.viewmodel.ChatUiState
import com.nigdroid.quantummessenger.presentation.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    participantId : String       = "participant_user",
    onBack        : () -> Unit   = {},
    viewModel     : ChatViewModel = hiltViewModel()
) {
    var messageInput by remember { mutableStateOf("") }
    val uiState      by viewModel.uiState.collectAsState()
    val focusManager  = LocalFocusManager.current
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.initialize("", participantId)
    }

    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Success) {
            val count = (uiState as ChatUiState.Success).messages.size
            if (count > 0) lazyListState.animateScrollToItem(count - 1)
        }
    }

    // currentUserId is loaded from SessionManager inside ChatViewModel
    val currentUserId = (uiState as? ChatUiState.Success)?.currentUserId ?: ""

    var showClearChatDialog by remember { mutableStateOf(false) }
    var showSaveContactDialog by remember { mutableStateOf(false) }
    var showRenameContactDialog by remember { mutableStateOf(false) }
    var contactNameInput by remember { mutableStateOf("") }

    // Clear chat confirmation dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            containerColor = QuantumColors.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    Modifier.size(52.dp)
                        .background(QuantumColors.Error.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DeleteSweep, null, tint = QuantumColors.Error, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Clear Chat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuantumColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "This will permanently delete all messages in this conversation. The contact will remain.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QuantumColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showClearChatDialog = false; viewModel.clearChat() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Error),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearChatDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, QuantumColors.GlassBorder),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Cancel", color = QuantumColors.TextSecondary)
                }
            }
        )
    }

    // Save contact dialog
    if (showSaveContactDialog) {
        AlertDialog(
            onDismissRequest = { showSaveContactDialog = false },
            containerColor = QuantumColors.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    Modifier.size(52.dp)
                        .background(QuantumColors.Primary.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = QuantumColors.Primary, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Save Contact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuantumColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        "Enter a name for this contact:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuantumColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextField(
                        value = contactNameInput,
                        onValueChange = { contactNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contact name", color = QuantumColors.TextTertiary) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = QuantumColors.GlassWhite08,
                            unfocusedContainerColor = QuantumColors.GlassWhite08,
                            focusedIndicatorColor = QuantumColors.Primary,
                            unfocusedIndicatorColor = QuantumColors.GlassBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveContactDialog = false
                        viewModel.saveContact(contactNameInput.ifBlank { null })
                        contactNameInput = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSaveContactDialog = false; contactNameInput = "" },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, QuantumColors.GlassBorder),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Cancel", color = QuantumColors.TextSecondary)
                }
            }
        )
    }

    // Rename contact dialog
    if (showRenameContactDialog) {
        AlertDialog(
            onDismissRequest = { showRenameContactDialog = false },
            containerColor = QuantumColors.Surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    Modifier.size(52.dp)
                        .background(QuantumColors.Primary.copy(alpha = 0.15f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = QuantumColors.Primary, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    "Rename Contact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuantumColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        "Enter new name for this contact:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuantumColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextField(
                        value = contactNameInput,
                        onValueChange = { contactNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contact name", color = QuantumColors.TextTertiary) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = QuantumColors.GlassWhite08,
                            unfocusedContainerColor = QuantumColors.GlassWhite08,
                            focusedIndicatorColor = QuantumColors.Primary,
                            unfocusedIndicatorColor = QuantumColors.GlassBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameContactDialog = false
                        viewModel.renameContact(contactNameInput)
                        contactNameInput = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Rename", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRenameContactDialog = false; contactNameInput = "" },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, QuantumColors.GlassBorder),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Cancel", color = QuantumColors.TextSecondary)
                }
            }
        )
    }

    ChatScreenContent(
        uiState       = uiState,
        currentUserId = currentUserId,
        participantId = participantId,
        messageInput  = messageInput,
        lazyListState = lazyListState,
        onBack        = onBack,
        onInputChange = { messageInput = it },
        onSend        = {
            if (messageInput.isNotBlank()) {
                viewModel.sendMessage(messageInput)
                messageInput = ""
                focusManager.clearFocus()
            }
        },
        onRetry       = { viewModel.retryLoadingMessages() },
        onClearChat   = { showClearChatDialog = true },
        onSaveContact = { showSaveContactDialog = true },
        onRenameContact = { 
            contactNameInput = (uiState as? ChatUiState.Success)?.contactName ?: ""
            showRenameContactDialog = true 
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen layout (decoupled for preview)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatScreenContent(
    uiState       : ChatUiState,
    currentUserId : String,
    participantId : String,
    messageInput  : String,
    lazyListState : LazyListState,
    onBack        : () -> Unit,
    onInputChange : (String) -> Unit,
    onSend        : () -> Unit,
    onRetry       : () -> Unit,
    onClearChat   : () -> Unit = {},
    onSaveContact : () -> Unit = {},
    onRenameContact : () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // Use imePadding on the main container to shift contents correctly
        ) {
            // ── Header ───────────────────────────────────────────────────────
            val contactName = (uiState as? ChatUiState.Success)?.contactName
            val isContactSaved = (uiState as? ChatUiState.Success)?.isContactSaved ?: false
            ChatHeader(
                participantInitial = (contactName?.firstOrNull() ?: participantId.firstOrNull())
                    ?.uppercaseChar()?.toString() ?: "?",
                participantName    = contactName ?: participantId.take(12) + "…",
                onBack             = onBack,
                onClearChat        = onClearChat,
                onSaveContact      = onSaveContact,
                onRenameContact    = onRenameContact,
                isContactSaved     = isContactSaved
            )

            // ── Content ──────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState   = uiState,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                    label         = "chatStateAnim",
                    modifier      = Modifier.fillMaxSize()
                ) { state ->
                    when (state) {
                        is ChatUiState.Loading -> ChatLoadingState()
                        is ChatUiState.Error   -> ChatErrorState(state.message, onRetry)
                        is ChatUiState.Success -> {
                            if (state.messages.isEmpty()) {
                                ChatEmptyState()
                            } else {
                                ChatMessageList(
                                    messages      = state.messages,
                                    currentUserId = currentUserId,
                                    lazyListState = lazyListState
                                )
                            }
                        }
                    }
                }
            }

            // ── Input bar (always visible) ───────────────────────────────────
            val isSending = (uiState as? ChatUiState.Success)?.isSending ?: false
            ChatInputBar(
                messageInput  = messageInput,
                onInputChange = onInputChange,
                onSend        = onSend,
                isSending     = isSending
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(
    participantInitial : String,
    participantName    : String,
    onBack             : () -> Unit,
    onClearChat        : () -> Unit = {},
    onSaveContact      : () -> Unit = {},
    onRenameContact    : () -> Unit = {},
    isContactSaved     : Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuantumColors.GlassWhite08)
    ) {
        Spacer(Modifier.statusBarsPadding())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick  = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint               = QuantumColors.TextPrimary
                )
            }

            Spacer(Modifier.width(4.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(QuantumColors.Primary, QuantumColors.Accent.copy(alpha = 0.7f))
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = participantInitial,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = participantName,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = QuantumColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(QuantumColors.Success, CircleShape)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text  = "End-to-end encrypted",
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantumColors.TextTertiary
                    )
                }
            }

            // Options
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector        = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint               = QuantumColors.TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = QuantumColors.Surface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Search", color = QuantumColors.TextPrimary) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.MoreVert, null, tint = QuantumColors.Primary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mute Notifications", color = QuantumColors.TextPrimary) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = QuantumColors.Primary) }
                    )
                    if (!isContactSaved) {
                        DropdownMenuItem(
                            text = { Text("Save Contact", color = QuantumColors.Primary) },
                            onClick = { showMenu = false; onSaveContact() },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = QuantumColors.Primary) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Rename Contact", color = QuantumColors.Primary) },
                            onClick = { showMenu = false; onRenameContact() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = QuantumColors.Primary) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Clear Chat", color = QuantumColors.Error) },
                        onClick = { showMenu = false; onClearChat() },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, null, tint = QuantumColors.Error) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatMessageList(
    messages      : List<ChatMessage>,
    currentUserId : String,
    lazyListState : LazyListState
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        state          = lazyListState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Group messages by date — insert date separators
        val grouped = messages.groupBy { msg ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(msg.timestamp))
        }
        grouped.forEach { (dateKey, msgs) ->
            // Date separator
            item(key = "date_$dateKey") {
                DateSeparator(timestamp = msgs.first().timestamp)
            }
            items(msgs, key = { it.id }) { message ->
                AnimatedVisibility(
                    visible       = true,
                    enter         = slideInVertically(
                        initialOffsetY = { 30 },
                        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn()
                ) {
                    MessageBubble(
                        message = message,
                        isOwn   = message.senderId == currentUserId
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(timestamp: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .glassmorphism(cornerRadius = 12, overlayAlpha = 0.08f)
                .background(QuantumColors.GlassWhite08, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            val today    = Calendar.getInstance()
            val msgCal   = Calendar.getInstance().apply { time = Date(timestamp) }
            val dateStr  = when {
                today.get(Calendar.DATE) == msgCal.get(Calendar.DATE)
                        && today.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Today"
                today.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1
                        && today.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
            Text(
                text  = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = QuantumColors.TextTertiary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessage, isOwn: Boolean) {
    Box(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwn) 48.dp else 0.dp,
                end   = if (isOwn) 0.dp  else 48.dp
            ),
        contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = if (isOwn) {
                Modifier
                    .glassmorphismBubbleOwn()
                    .clip(
                        RoundedCornerShape(
                            topStart     = 18.dp,
                            topEnd       = 4.dp,
                            bottomStart  = 18.dp,
                            bottomEnd    = 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            } else {
                Modifier
                    .glassmorphismBubbleOther()
                    .background(
                        color = QuantumColors.GlassWhite08,
                        shape = RoundedCornerShape(
                            topStart     = 4.dp,
                            topEnd       = 18.dp,
                            bottomStart  = 18.dp,
                            bottomEnd    = 18.dp
                        )
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart     = 4.dp,
                            topEnd       = 18.dp,
                            bottomStart  = 18.dp,
                            bottomEnd    = 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            },
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
        ) {
            Text(
                text      = message.content,
                style     = MaterialTheme.typography.bodyLarge.copy(
                    color  = QuantumColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment  = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text  = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOwn) Color.White.copy(alpha = 0.6f) else QuantumColors.TextTertiary,
                    fontSize = 10.sp
                )
                if (isOwn) {
                    val (icon, tint) = when (message.status) {
                        MessageStatus.PENDING -> Icons.Default.AccessTime to QuantumColors.TextTertiary
                        MessageStatus.SENT    -> Icons.Default.Check       to QuantumColors.Teal
                        MessageStatus.ERROR   -> Icons.Default.ErrorOutline to QuantumColors.Error
                    }
                    Icon(
                        imageVector        = icon,
                        contentDescription = message.status.name,
                        tint               = tint,
                        modifier           = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    messageInput  : String,
    onInputChange : (String) -> Unit,
    onSend        : () -> Unit,
    isSending     : Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuantumColors.GlassWhite08)
            .navigationBarsPadding() // Keep input bar above system nav
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Text field ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp, max = 120.dp)
                .glassmorphism(cornerRadius = 24, overlayAlpha = 0.10f)
                .background(QuantumColors.SurfaceInput, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            TextField(
                value         = messageInput,
                onValueChange = onInputChange,
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = {
                    Text(
                        "Message…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuantumColors.TextTertiary
                    )
                },
                maxLines      = 4,
                singleLine    = false,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType   = KeyboardType.Text,
                    imeAction      = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { if (!isSending) onSend() }),
                enabled         = !isSending,
                textStyle       = MaterialTheme.typography.bodyLarge.copy(
                    color    = QuantumColors.TextPrimary,
                    fontSize = 15.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = QuantumColors.Primary
                )
            )
        }

        // ── Send button ──────────────────────────────────────────────────────
        val sendEnabled = messageInput.isNotBlank() && !isSending
        val buttonScale by animateFloatAsState(
            targetValue   = if (sendEnabled) 1f else 0.85f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy),
            label         = "sendBtnScale"
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(buttonScale)
                .background(
                    brush = if (sendEnabled) Brush.linearGradient(
                        colors = listOf(QuantumColors.Primary, QuantumColors.Accent.copy(alpha = 0.8f))
                    ) else Brush.linearGradient(
                        colors = listOf(QuantumColors.TextDisabled, QuantumColors.TextDisabled)
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(enabled = sendEnabled, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    color       = Color.White,
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector        = Icons.Default.Send,
                    contentDescription = "Send",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty / Loading / Error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyState() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("🔐", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "Quantum Safe",
            style = MaterialTheme.typography.titleMedium,
            color = QuantumColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Send the first message.\nYour conversation is fully encrypted.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = QuantumColors.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatLoadingState() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color       = QuantumColors.Primary,
            strokeWidth = 3.dp,
            modifier    = Modifier.size(46.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "Decrypting messages…",
            style = MaterialTheme.typography.bodyMedium,
            color = QuantumColors.TextTertiary
        )
    }
}

@Composable
private fun ChatErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "Connection error",
            style = MaterialTheme.typography.titleLarge,
            color = QuantumColors.Error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = QuantumColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick  = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary)
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

private val sampleMessages = listOf(
    ChatMessage(id = 1L, senderId = "other",        receiverId = "me", content = "Hey! Is this quantum-encrypted? 🔐",      timestamp = System.currentTimeMillis() - 180_000, status = MessageStatus.SENT, messageType = MessageType.TEXT),
    ChatMessage(id = 2L, senderId = "current_user", receiverId = "other", content = "Yes! Post-quantum CRYSTALS-Kyber key exchange. Nobody can read it.", timestamp = System.currentTimeMillis() - 120_000, status = MessageStatus.SENT, messageType = MessageType.TEXT),
    ChatMessage(id = 3L, senderId = "other",        receiverId = "me", content = "That's incredible. Even quantum computers can't break it?", timestamp = System.currentTimeMillis() - 90_000, status = MessageStatus.SENT, messageType = MessageType.TEXT),
    ChatMessage(id = 4L, senderId = "current_user", receiverId = "other", content = "Exactly. That's the whole point 🛡️", timestamp = System.currentTimeMillis() - 30_000, status = MessageStatus.PENDING, messageType = MessageType.TEXT),
)

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Chat — Messages", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewChatMessages() {
    QuantumMessengerTheme {
        ChatScreenContent(
            uiState       = ChatUiState.Success(sampleMessages),
            currentUserId = "current_user",
            participantId = "other",
            messageInput  = "Type something…",
            lazyListState = rememberLazyListState(),
            onBack        = {},
            onInputChange = {},
            onSend        = {},
            onRetry       = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Chat — Empty", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewChatEmpty() {
    QuantumMessengerTheme {
        ChatScreenContent(
            uiState       = ChatUiState.Success(emptyList()),
            currentUserId = "current_user",
            participantId = "other",
            messageInput  = "",
            lazyListState = rememberLazyListState(),
            onBack        = {},
            onInputChange = {},
            onSend        = {},
            onRetry       = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Chat — Loading", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewChatLoading() {
    QuantumMessengerTheme {
        ChatScreenContent(
            uiState       = ChatUiState.Loading,
            currentUserId = "current_user",
            participantId = "other",
            messageInput  = "",
            lazyListState = rememberLazyListState(),
            onBack        = {},
            onInputChange = {},
            onSend        = {},
            onRetry       = {}
        )
    }
}

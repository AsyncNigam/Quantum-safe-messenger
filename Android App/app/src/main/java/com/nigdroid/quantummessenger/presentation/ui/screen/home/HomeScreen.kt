package com.nigdroid.quantummessenger.presentation.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nigdroid.quantummessenger.R
import com.nigdroid.quantummessenger.data.local.ContactEntity
import com.nigdroid.quantummessenger.domain.model.InboxItem
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.HomeUiState
import com.nigdroid.quantummessenger.presentation.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: HomeViewModel  = hiltViewModel(),
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreenContent(
        uiState        = uiState,
        onChatClick    = onChatClick,
        onNewChatClick = onNewChatClick,
        onRetry        = { viewModel.syncContacts() },
        onSearchQuery  = { viewModel.updateSearchQuery(it) }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen layout (decoupled for preview)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onRetry: () -> Unit,
    onSearchQuery: (String) -> Unit = {}
) {
    var showSearch by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                HomeTopBar(
                    onSearchClick = { showSearch = !showSearch },
                    onMenuClick   = { showMenu = !showMenu },
                    showMenu      = showMenu,
                    onDismissMenu = { showMenu = false },
                    onNewChatClick = onNewChatClick
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Search bar ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = showSearch,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    val query = (uiState as? HomeUiState.Success)?.searchQuery ?: ""
                    SearchBar(
                        query    = query,
                        onQuery  = onSearchQuery,
                        onClose  = { showSearch = false; onSearchQuery("") }
                    )
                }

                // ── Main content ────────────────────────────────────────
                AnimatedContent(
                    targetState   = uiState,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(200))
                    },
                    label    = "homeStateAnim",
                    modifier = Modifier.weight(1f)
                ) { state ->
                    when (state) {
                        is HomeUiState.Loading -> HomeLoadingState()
                        is HomeUiState.Error   -> HomeErrorState(state.message, onRetry)
                        is HomeUiState.Success -> {
                            if (state.inboxItems.isEmpty() && state.contacts.isEmpty()) {
                                HomeEmptyState(onNewChatClick)
                            } else {
                                HomeConversationList(
                                    inboxItems  = state.inboxItems,
                                    contacts    = state.contacts,
                                    onChatClick = onChatClick
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            NewChatFab(onClick = onNewChatClick)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    showMenu: Boolean = false,
    onDismissMenu: () -> Unit = {},
    onNewChatClick: () -> Unit = {}
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
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.qlogo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Quantum Safe",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuantumColors.TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TopBarIconButton(Icons.Default.Search, "Search", onSearchClick)
                Box {
                    TopBarIconButton(Icons.Default.MoreVert, "Menu", onMenuClick)
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = onDismissMenu,
                        containerColor = QuantumColors.Surface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Chat", color = QuantumColors.TextPrimary) },
                            onClick = { onDismissMenu(); onNewChatClick() },
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = QuantumColors.Primary) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassmorphism(cornerRadius = 16, overlayAlpha = 0.10f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = QuantumColors.TextTertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        TextField(
            value = query, onValueChange = onQuery,
            placeholder = { Text("Search contacts…", color = QuantumColors.TextTertiary) },
            singleLine = true, modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = QuantumColors.TextPrimary),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                cursorColor = QuantumColors.Primary
            )
        )
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, "Close", tint = QuantumColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TopBarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit = {}) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
            .glassmorphism(cornerRadius = 12, overlayAlpha = 0.10f)
            .background(color = QuantumColors.GlassWhite08, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Icon(icon, description, tint = QuantumColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NewChatFab(onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "fabScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(60.dp)
            .glassmorphism(cornerRadius = 30, overlayAlpha = 0.18f, usePrimaryTint = true)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(QuantumColors.Primary, QuantumColors.Accent.copy(alpha = 0.8f))
                ),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Add,
            contentDescription = "New chat",
            tint               = Color.White,
            modifier           = Modifier.size(26.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Conversation list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeConversationList(
    inboxItems: List<InboxItem>,
    contacts: List<ContactEntity>,
    onChatClick: (String) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start  = 16.dp,
            end    = 16.dp,
            top    = 8.dp,
            bottom = 100.dp   // FAB clearance
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Conversations section ─────────────────────────────────────────
        if (inboxItems.isNotEmpty()) {
            item {
                Text(
                    text     = "Chats",
                    style    = MaterialTheme.typography.titleSmall,
                    color    = QuantumColors.TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
            items(inboxItems, key = { it.userId }) { item ->
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                ) {
                    InboxListItem(item = item, onClick = { onChatClick(item.userId) })
                }
            }
        }

        // ── Contacts without conversations ────────────────────────────────
        if (contacts.isNotEmpty()) {
            item {
                Text(
                    text     = "Contacts",
                    style    = MaterialTheme.typography.titleLarge,
                    color    = QuantumColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            items(contacts, key = { "contact_${it.userId}" }) { contact ->
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                ) {
                    ContactListItem(
                        contact = contact,
                        onClick = { onChatClick(contact.userId) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contact item card (no messages yet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContactListItem(contact: ContactEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.12f)
            .background(
                color = QuantumColors.GlassWhite08,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                QuantumColors.Accent.copy(alpha = 0.6f),
                                QuantumColors.Primary.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = contact.displayName?.firstOrNull()?.toString()?.uppercase()
                                 ?: contact.userId.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = contact.displayName ?: contact.userId.take(12) + "…",
                    style     = MaterialTheme.typography.titleMedium,
                    color     = QuantumColors.TextPrimary,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Tap to start chatting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QuantumColors.TextTertiary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inbox item card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InboxListItem(
    item: InboxItem,
    onClick: () -> Unit,
    isDarkTheme: Boolean = true   // kept for backward compat
) {
    val isPressed = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed.value) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "itemScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.12f)
            .background(
                color = QuantumColors.GlassWhite08,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // ── Avatar ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                QuantumColors.Primary.copy(alpha = 0.8f),
                                QuantumColors.Accent.copy(alpha  = 0.6f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = item.displayName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            // ── Text content ─────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text      = item.displayName ?: "Unknown",
                        style     = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color     = QuantumColors.TextPrimary,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text  = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = QuantumColors.TextTertiary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = item.lastMessage,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = if (item.unreadCount > 0) QuantumColors.TextSecondary
                                   else QuantumColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(QuantumColors.Primary, QuantumColors.Accent)
                                    ),
                                    shape = CircleShape
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty / Loading / Error states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(onNewChatClick: (() -> Unit)? = null) = HomeEmptyState(onNewChatClick)

@Composable
private fun HomeEmptyState(onNewChatClick: (() -> Unit)? = null) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        // Placeholder illustration — encrypted lock emoji in glass orb
        Box(
            modifier = Modifier
                .size(100.dp)
                .glassmorphism(cornerRadius = 50, overlayAlpha = 0.12f, usePrimaryTint = true)
                .background(QuantumColors.GlassWhite08, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("💬", fontSize = 42.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text      = "No conversations yet",
            style     = MaterialTheme.typography.titleLarge,
            color     = QuantumColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text      = "Tap + to start your first\nQuantum Safe conversation.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = QuantumColors.TextTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (onNewChatClick != null) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick  = onNewChatClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary)
            ) {
                Text("Start a Conversation", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeLoadingState() {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color        = QuantumColors.Primary,
            strokeWidth  = 3.dp,
            modifier     = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = "Loading conversations…",
            style = MaterialTheme.typography.bodyMedium,
            color = QuantumColors.TextTertiary
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) = HomeErrorState(message, onRetry)

@Composable
private fun HomeErrorState(message: String, onRetry: () -> Unit) {
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
            text  = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = QuantumColors.Error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = QuantumColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

private fun formatTimestamp(timestamp: Long): String {
    val date    = Date(timestamp)
    val now     = Calendar.getInstance()
    val msgDate = Calendar.getInstance().apply { time = date }
    return if (now.get(Calendar.DATE) == msgDate.get(Calendar.DATE)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

private val sampleInboxItems = listOf(
    InboxItem(userId = "1", displayName = "Alice Johnson",  lastMessage = "Hey, did you get my file? 🔐", timestamp = System.currentTimeMillis() - 120_000,  unreadCount = 3),
    InboxItem(userId = "2", displayName = "Bob Martinez",  lastMessage = "Meeting is at 3pm confirmed",   timestamp = System.currentTimeMillis() - 3_600_000, unreadCount = 0),
    InboxItem(userId = "3", displayName = "Carol Tan",     lastMessage = "Sure, I'll review the keys",    timestamp = System.currentTimeMillis() - 86_400_000, unreadCount = 1),
    InboxItem(userId = "4", displayName = "David Kim",     lastMessage = "Quantum key exchanged ✓",       timestamp = System.currentTimeMillis() - 172_800_000, unreadCount = 0),
)

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Home — Chats list", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewHomeConversations() {
    QuantumMessengerTheme {
        HomeScreenContent(
            uiState        = HomeUiState.Success(sampleInboxItems),
            onChatClick    = {},
            onNewChatClick = {},
            onRetry        = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Home — Empty", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewHomeEmpty() {
    QuantumMessengerTheme {
        HomeScreenContent(
            uiState        = HomeUiState.Success(emptyList()),
            onChatClick    = {},
            onNewChatClick = {},
            onRetry        = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Home — Loading", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewHomeLoading() {
    QuantumMessengerTheme {
        HomeScreenContent(
            uiState        = HomeUiState.Loading,
            onChatClick    = {},
            onNewChatClick = {},
            onRetry        = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08070E, name = "Home — Error", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewHomeError() {
    QuantumMessengerTheme {
        HomeScreenContent(
            uiState        = HomeUiState.Error("Unable to sync contacts. Check your connection."),
            onChatClick    = {},
            onNewChatClick = {},
            onRetry        = {}
        )
    }
}

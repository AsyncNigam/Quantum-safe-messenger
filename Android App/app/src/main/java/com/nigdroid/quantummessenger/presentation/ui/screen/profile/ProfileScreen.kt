package com.nigdroid.quantummessenger.presentation.ui.screen.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.AccountActionState
import com.nigdroid.quantummessenger.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onAccountCleared: () -> Unit = {}
) {
    val fingerprint by viewModel.fingerprint.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val mlKemKey    by viewModel.mlKemPublicKey.collectAsState()
    val x25519Key   by viewModel.x25519PublicKey.collectAsState()
    val isEditing   by viewModel.isEditing.collectAsState()
    val accountAction by viewModel.accountAction.collectAsState()

    var nameInput by remember(displayName) { mutableStateOf(displayName ?: "") }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // React to account action results
    LaunchedEffect(accountAction) {
        when (accountAction) {
            is AccountActionState.Success -> {
                viewModel.resetAccountAction()
                onAccountCleared()
            }
            is AccountActionState.Error -> {
                Toast.makeText(
                    context,
                    (accountAction as AccountActionState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetAccountAction()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QuantumColors.GlassWhite08)
                ) {
                    Spacer(Modifier.statusBarsPadding())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(64.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Identity",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuantumColors.TextPrimary
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Zero-Knowledge profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QuantumColors.TextTertiary
                )
                Spacer(Modifier.height(28.dp))

                // ── Avatar orb ───────────────────────────────────────────
                ProfileAvatar(displayName = displayName)
                Spacer(Modifier.height(24.dp))

                // ── Display Name ─────────────────────────────────────────
                DisplayNameCard(
                    displayName = displayName,
                    nameInput = nameInput,
                    isEditing = isEditing,
                    onNameChange = { nameInput = it },
                    onSave = { viewModel.saveDisplayName(nameInput) },
                    onEdit = { viewModel.startEditing() },
                    onCancel = {
                        nameInput = displayName ?: ""
                        viewModel.cancelEditing()
                    }
                )
                Spacer(Modifier.height(16.dp))

                // ── QR Code ──────────────────────────────────────────────
                if (fingerprint != null) {
                    QrCodeCard(fingerprint = fingerprint!!)
                    Spacer(Modifier.height(16.dp))
                }

                // ── Public Keys ──────────────────────────────────────────
                PublicKeysCard(
                    fingerprint = fingerprint ?: "Not registered",
                    mlKemKey    = mlKemKey,
                    x25519Key   = x25519Key
                )
                Spacer(Modifier.height(16.dp))

                // ── About PQC Card ───────────────────────────────────────
                AboutPqcCard(onClick = { showAboutSheet = true })
                Spacer(Modifier.height(24.dp))

                // ── Account Actions ─────────────────────────────────────
                AccountActionsCard(
                    isLoading = accountAction is AccountActionState.Loading,
                    onLogoutClick = { showLogoutDialog = true },
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        }
    }

    // ── About Bottom Sheet ───────────────────────────────────────────────
    if (showAboutSheet) {
        AboutPqcBottomSheet(onDismiss = { showAboutSheet = false })
    }

    // ── Logout Confirmation Dialog ──────────────────────────────────────
    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Logout",
            message = "You will be logged out and taken to the registration screen.\n\n" +
                    "Your keys, messages and contacts will stay on this device. " +
                    "Tap \u201cGenerate Identity\u201d to log back into the same account.",
            confirmText = "Logout",
            confirmColor = QuantumColors.Warning,
            icon = Icons.AutoMirrored.Filled.Logout,
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    // ── Delete Account Confirmation Dialog ──────────────────────────────
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Account",
            message = "This is irreversible. Your account will be permanently deleted:\n\n" +
                    "\u2022 Public keys wiped from the server\n" +
                    "\u2022 All local data (messages, contacts, keys) erased\n" +
                    "\u2022 Contacts will see you as \"Deleted Account\"\n\n" +
                    "You cannot recover this identity.",
            confirmText = "Delete Forever",
            confirmColor = QuantumColors.Error,
            icon = Icons.Default.DeleteForever,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Avatar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatar(displayName: String?) {
    val inf = rememberInfiniteTransition(label = "avatar")
    val glow by inf.animateFloat(0.25f, 0.55f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
        Box(Modifier.size(120.dp).background(
            Brush.radialGradient(listOf(QuantumColors.Primary.copy(alpha = glow), Color.Transparent)),
            CircleShape))
        Box(
            Modifier.size(90.dp)
                .glassmorphism(cornerRadius = 45, overlayAlpha = 0.18f, usePrimaryTint = true)
                .background(Brush.linearGradient(
                    listOf(QuantumColors.Primary.copy(alpha = 0.5f), QuantumColors.Accent.copy(alpha = 0.3f)),
                    start = Offset.Zero, end = Offset(90f, 90f)), CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName?.firstOrNull()?.uppercase() ?: "?",
                fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Display Name Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DisplayNameCard(
    displayName: String?,
    nameInput: String,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, tint = QuantumColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Display Name", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
            }
            Text("Local only — never sent to the server.", style = MaterialTheme.typography.bodySmall,
                color = QuantumColors.TextTertiary)

            if (isEditing) {
                // ── Edit Mode ────────────────────────────────────────────
                OutlinedTextField(
                    value = nameInput, onValueChange = onNameChange,
                    placeholder = { Text("Anonymous", color = QuantumColors.TextTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuantumColors.Primary,
                        unfocusedBorderColor = QuantumColors.GlassBorder,
                        cursorColor = QuantumColors.Primary,
                        focusedTextColor = QuantumColors.TextPrimary,
                        unfocusedTextColor = QuantumColors.TextPrimary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, QuantumColors.GlassBorder)
                    ) { Text("Cancel", color = QuantumColors.TextSecondary) }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary)
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            } else {
                // ── View Mode ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(cornerRadius = 14, overlayAlpha = 0.06f)
                        .background(QuantumColors.GlassWhite08, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName ?: "Anonymous",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (displayName != null) QuantumColors.TextPrimary
                                else QuantumColors.TextTertiary
                    )
                    Icon(
                        Icons.Default.Edit, "Edit",
                        tint = QuantumColors.Primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR Code Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrCodeCard(fingerprint: String) {
    val qrBitmap = remember(fingerprint) { generateQrBitmap(fingerprint, 512) }

    Box(
        Modifier.fillMaxWidth()
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(), // ← add this
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = QuantumColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Identity QR Code", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
            }
            Text(
                text = "Others can scan this to add you as a contact.",
                style = MaterialTheme.typography.bodySmall,
                color = QuantumColors.TextTertiary,
                textAlign = TextAlign.Center, // ← center the subtitle too
                modifier = Modifier.fillMaxWidth()
            )
            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally) // ← explicit centering
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // Fingerprint hex preview
            Text(
                text = fingerprint.take(16).chunked(4).joinToString(" ").uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                color = QuantumColors.Primary
            )
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits.get(x, y)) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (_: Exception) { null }
}

// ─────────────────────────────────────────────────────────────────────────────
// Public Keys Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PublicKeysCard(fingerprint: String, mlKemKey: String, x25519Key: String) {
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxWidth()
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, null, tint = QuantumColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cryptographic Keys", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
            }
            KeyRow("Text Fingerprint", fingerprint, ctx)
            KeyRow("ML-KEM-768", mlKemKey, ctx)
            KeyRow("X25519", x25519Key, ctx)
        }
    }
}

@Composable
private fun KeyRow(label: String, value: String, ctx: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = QuantumColors.TextTertiary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (value.length > 24) value.take(12) + "…" + value.takeLast(12) else value,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = QuantumColors.TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(ctx, "$label copied", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, "Copy", tint = QuantumColors.Primary,
                    modifier = Modifier.size(16.dp))
            }
        }
        if (label != "X25519") {
            HorizontalDivider(color = QuantumColors.GlassBorder, thickness = 0.5.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About Quantum Safe Card (tap → bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutPqcCard(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(
                Brush.linearGradient(
                    listOf(QuantumColors.Primary.copy(alpha = 0.12f), QuantumColors.Accent.copy(alpha = 0.08f))),
                RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp)
                    .background(QuantumColors.Primary.copy(alpha = 0.20f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Info, null, tint = QuantumColors.Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("About Quantum Safe", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
                Text("Quantum safe encryption details", style = MaterialTheme.typography.bodySmall,
                    color = QuantumColors.TextTertiary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = QuantumColors.TextTertiary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About Bottom Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutPqcBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = QuantumColors.Surface,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp)
                .width(40.dp).height(4.dp)
                .background(QuantumColors.GlassBorder, RoundedCornerShape(2.dp)))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Shield icon ──────────────────────────────────────────────
            Box(
                Modifier.size(64.dp)
                    .background(
                        Brush.linearGradient(listOf(QuantumColors.Primary, QuantumColors.Accent)),
                        CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Text("Quantum Safe", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = QuantumColors.TextPrimary,
                textAlign = TextAlign.Center)

            // ── Encryption cards ─────────────────────────────────────────
            EncryptionDetailCard(
                icon = "⚛️", title = "ML-KEM-768 (CRYSTALS-Kyber)",
                desc = "NIST FIPS 203 — Post-quantum Key Encapsulation Mechanism. " +
                        "Provides quantum-resistant key exchange using lattice-based cryptography. " +
                        "768-dimensional module lattice ensures security against both classical and quantum attacks."
            )
            EncryptionDetailCard(
                icon = "🔐", title = "AES-256-GCM",
                desc = "NIST-approved symmetric encryption with 256-bit keys and Galois/Counter Mode. " +
                        "Provides authenticated encryption ensuring both confidentiality and integrity of every message."
            )
            EncryptionDetailCard(
                icon = "🔑", title = "X25519 (Curve25519 DH)",
                desc = "Elliptic-curve Diffie-Hellman key agreement. Combined with ML-KEM in a hybrid " +
                        "construction for defense-in-depth — secure even if one primitive is broken."
            )
            EncryptionDetailCard(
                icon = "🛡️", title = "Zero-Knowledge Identity",
                desc = "No email, no phone, no name. Your identity is a SHA-256 hash of your public keys. " +
                        "The server never learns who you are — it only stores opaque cryptographic material."
            )

            HorizontalDivider(color = QuantumColors.GlassBorder, thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp))

            // ── Credit ───────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Architected and Built by",
                    style = MaterialTheme.typography.bodySmall, color = QuantumColors.TextTertiary)
                Text("Nigam Prasad Sahoo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuantumColors.Primary)
                Text("Quantum-safe messaging for the post-quantum era",
                    style = MaterialTheme.typography.labelSmall,
                    color = QuantumColors.TextTertiary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun EncryptionDetailCard(icon: String, title: String, desc: String) {
    Box(
        Modifier.fillMaxWidth()
            .glassmorphism(cornerRadius = 16, overlayAlpha = 0.08f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
            }
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = QuantumColors.TextSecondary, lineHeight = 18.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Account Actions Card (Logout + Delete)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountActionsCard(
    isLoading: Boolean,
    onLogoutClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
            .background(QuantumColors.GlassWhite08, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ManageAccounts, null, tint = QuantumColors.Primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Account", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
            }
            Text(
                "Manage your anonymous identity.",
                style = MaterialTheme.typography.bodySmall,
                color = QuantumColors.TextTertiary
            )

            // ── Logout Button ────────────────────────────────────────────
            OutlinedButton(
                onClick = onLogoutClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, QuantumColors.Warning.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = QuantumColors.Warning
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = QuantumColors.Warning
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.SemiBold)
            }

            // ── Delete Account Button ────────────────────────────────────
            OutlinedButton(
                onClick = onDeleteClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, QuantumColors.Error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = QuantumColors.Error
                )
            ) {
                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Account", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Confirmation Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = QuantumColors.Surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                Modifier.size(52.dp)
                    .background(confirmColor.copy(alpha = 0.15f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = confirmColor, modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = QuantumColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = QuantumColors.TextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                modifier = Modifier.height(44.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, QuantumColors.GlassBorder),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Cancel", color = QuantumColors.TextSecondary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF08070E, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewProfile() {
    QuantumMessengerTheme {
        Box(Modifier.fillMaxSize()) {
            AnimatedMeshGradientBackground(Modifier.fillMaxSize())
            Column(
                Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                Text("Identity", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = QuantumColors.TextPrimary)
                Spacer(Modifier.height(28.dp))
                ProfileAvatar("Nigam")
                Spacer(Modifier.height(24.dp))
                DisplayNameCard("Nigam", "Nigam", false, {}, {}, {}, {})
                Spacer(Modifier.height(16.dp))
                QrCodeCard("a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890")
                Spacer(Modifier.height(16.dp))
                PublicKeysCard("a1b2c3d4…", "(keystore)", "(keystore)")
                Spacer(Modifier.height(16.dp))
                AboutPqcCard {}
            }
        }
    }
}

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
import com.nigdroid.quantummessenger.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val fingerprint by viewModel.fingerprint.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val mlKemKey    by viewModel.mlKemPublicKey.collectAsState()
    val x25519Key   by viewModel.x25519PublicKey.collectAsState()

    var nameInput by remember(displayName) { mutableStateOf(displayName ?: "") }
    var showAboutSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            // ── Header ───────────────────────────────────────────────
            Text("Identity", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, color = QuantumColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Your Zero-Knowledge profile", style = MaterialTheme.typography.bodyMedium,
                color = QuantumColors.TextTertiary)
            Spacer(Modifier.height(28.dp))

            // ── Avatar orb ───────────────────────────────────────────
            ProfileAvatar(displayName = displayName)
            Spacer(Modifier.height(24.dp))

            // ── Display Name ─────────────────────────────────────────
            DisplayNameCard(
                nameInput = nameInput,
                onNameChange = { nameInput = it },
                onSave = { viewModel.saveDisplayName(nameInput) }
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
        }
    }

    // ── About Bottom Sheet ───────────────────────────────────────────────
    if (showAboutSheet) {
        AboutPqcBottomSheet(onDismiss = { showAboutSheet = false })
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
private fun DisplayNameCard(nameInput: String, onNameChange: (String) -> Unit, onSave: () -> Unit) {
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
            Button(
                onClick = onSave, modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = QuantumColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Identity QR Code", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
            }
            Text("Others can scan this to add you as a contact.",
                style = MaterialTheme.typography.bodySmall, color = QuantumColors.TextTertiary)
            if (qrBitmap != null) {
                Box(
                    Modifier.size(200.dp)
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
// About PQC Card (tap → bottom sheet)
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
                Text("About PQC Chat", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
                Text("Post-quantum encryption details", style = MaterialTheme.typography.bodySmall,
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
        containerColor   = Color(0xFF110F1E),
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp)
                .width(40.dp).height(4.dp)
                .background(QuantumColors.GlassBorder, RoundedCornerShape(2.dp)))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 40.dp),
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

            Text("Post-Quantum Encryption", style = MaterialTheme.typography.headlineSmall,
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
                DisplayNameCard("Nigam", {}, {})
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

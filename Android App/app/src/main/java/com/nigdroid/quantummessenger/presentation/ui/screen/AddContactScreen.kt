package com.nigdroid.quantummessenger.presentation.ui.screen

import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism
import com.nigdroid.quantummessenger.presentation.viewmodel.AddContactUiState
import com.nigdroid.quantummessenger.presentation.viewmodel.AddContactViewModel

@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel = hiltViewModel(),
    onContactAdded: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualFingerprint by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var pendingQrFingerprint by remember { mutableStateOf<String?>(null) }
    var qrNameInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) hasCameraPermission = true
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Navigate on success
    LaunchedEffect(uiState) {
        when (uiState) {
            is AddContactUiState.Success -> {
                onContactAdded((uiState as AddContactUiState.Success).fingerprint)
                viewModel.resetState()
            }
            is AddContactUiState.AlreadyExists -> {
                onContactAdded((uiState as AddContactUiState.AlreadyExists).fingerprint)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Name prompt dialog after QR scan
    if (pendingQrFingerprint != null) {
        AlertDialog(
            onDismissRequest = { pendingQrFingerprint = null },
            title = { Text("Name this contact", color = QuantumColors.TextPrimary) },
            text = {
                Column {
                    Text(
                        "Scanned: ${pendingQrFingerprint!!.take(12)}…",
                        color = QuantumColors.TextTertiary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = qrNameInput,
                        onValueChange = { qrNameInput = it },
                        label = { Text("Contact name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = QuantumColors.TextPrimary,
                            unfocusedTextColor = QuantumColors.TextPrimary,
                            focusedBorderColor = QuantumColors.Primary,
                            unfocusedBorderColor = QuantumColors.GlassBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addContact(pendingQrFingerprint!!, qrNameInput)
                    pendingQrFingerprint = null
                    qrNameInput = ""
                }) { Text("Add", color = QuantumColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addContact(pendingQrFingerprint!!)
                    pendingQrFingerprint = null
                    qrNameInput = ""
                }) { Text("Skip", color = QuantumColors.TextSecondary) }
            },
            containerColor = QuantumColors.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Camera viewfinder ────────────────────────────────────────
        if (hasCameraPermission && !showManualInput) {
            QrScannerCamera(
                onQrScanned = { fingerprint ->
                    pendingQrFingerprint = fingerprint
                }
            )
        }

        // ── Dark overlay with reticle cutout ─────────────────────────
        if (hasCameraPermission && !showManualInput) {
            ScannerOverlay()
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                ScannerTopBar(
                    showManualInput = showManualInput,
                    onToggleMode = { showManualInput = !showManualInput }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showManualInput || !hasCameraPermission) {
                    ManualInputSection(
                        fingerprint = manualFingerprint,
                        onFingerprintChange = { manualFingerprint = it },
                        contactName = contactName,
                        onContactNameChange = { contactName = it },
                        onSubmit = { viewModel.addContact(manualFingerprint, contactName) },
                        isLoading = uiState is AddContactUiState.Loading,
                        noCameraPermission = !hasCameraPermission
                    )
                }
            }
        }

        // ── Bottom status bar ────────────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            StatusCard(uiState = uiState, onDismiss = { viewModel.resetState() })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraX + ML Kit QR Scanner
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScannerCamera(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScanning.getClient() }
    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Ensure we unbind when the composable is removed from the screen
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderState.value?.unbindAll()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderState.value = cameraProvider

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                        barcode.rawValue?.let { value ->
                                            if (value.length == 64 && value.matches(Regex("^[a-f0-9]+$"))) {
                                                onQrScanned(value)
                                            }
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    // Use a more specific selector to avoid issues with sub-cameras (like ID 3)
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("QrScanner", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Scanner overlay with animated reticle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerOverlay() {
    val isDark = isSystemInDarkTheme()
    val inf = rememberInfiniteTransition(label = "reticle")
    val scanLine by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLine"
    )
    val cornerAlpha by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cornerAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val reticleSize = w * 0.65f
        val left = (w - reticleSize) / 2f
        val top = (h - reticleSize) / 2f - 40.dp.toPx()
        val right = left + reticleSize
        val bottom = top + reticleSize
        val cornerLen = 32.dp.toPx()
        val cornerR = 12.dp.toPx()

        // Dim everything outside the reticle
        val path = Path().apply {
            addRect(Rect(0f, 0f, w, h))
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left, top, right, bottom, CornerRadius(cornerR)
                )
            )
            fillType = PathFillType.EvenOdd
        }
        val overlayColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.35f)
        drawPath(path, overlayColor)

        // Corner brackets
        val cornerColor = QuantumColors.Primary.copy(alpha = cornerAlpha)
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

        // Top-left
        drawLine(cornerColor, Offset(left, top + cornerLen), Offset(left, top + cornerR), stroke.width)
        drawLine(cornerColor, Offset(left + cornerR, top), Offset(left + cornerLen, top), stroke.width)
        // Top-right
        drawLine(cornerColor, Offset(right, top + cornerLen), Offset(right, top + cornerR), stroke.width)
        drawLine(cornerColor, Offset(right - cornerR, top), Offset(right - cornerLen, top), stroke.width)
        // Bottom-left
        drawLine(cornerColor, Offset(left, bottom - cornerLen), Offset(left, bottom - cornerR), stroke.width)
        drawLine(cornerColor, Offset(left + cornerR, bottom), Offset(left + cornerLen, bottom), stroke.width)
        // Bottom-right
        drawLine(cornerColor, Offset(right, bottom - cornerLen), Offset(right, bottom - cornerR), stroke.width)
        drawLine(cornerColor, Offset(right - cornerR, bottom), Offset(right - cornerLen, bottom), stroke.width)

        // Animated scan line
        val lineY = top + scanLine * reticleSize
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, QuantumColors.Primary.copy(alpha = 0.7f), Color.Transparent),
                startX = left, endX = right
            ),
            start = Offset(left + 8.dp.toPx(), lineY),
            end = Offset(right - 8.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerTopBar(showManualInput: Boolean, onToggleMode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuantumColors.GlassWhite08)
    ) {
        Spacer(Modifier.statusBarsPadding())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Add Contact", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold, color = QuantumColors.TextPrimary)
                Text(
                    if (showManualInput) "Enter fingerprint manually" else "Point camera at a QR code",
                    style = MaterialTheme.typography.bodySmall, color = QuantumColors.TextSecondary
                )
            }
            IconButton(
                onClick = onToggleMode,
                modifier = Modifier.size(44.dp)
                    .glassmorphism(cornerRadius = 14, overlayAlpha = 0.15f)
                    .background(QuantumColors.GlassWhite12, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Icon(
                    if (showManualInput) Icons.Default.CameraAlt else Icons.Default.Keyboard,
                    contentDescription = "Toggle mode", tint = QuantumColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Manual input fallback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ManualInputSection(
    fingerprint: String,
    onFingerprintChange: (String) -> Unit,
    contactName: String = "",
    onContactNameChange: (String) -> Unit = {},
    onSubmit: () -> Unit,
    isLoading: Boolean,
    noCameraPermission: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantumColors.Background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (noCameraPermission) {
            Icon(Icons.Default.CameraAlt, null, tint = QuantumColors.TextTertiary,
                modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Camera permission denied", style = MaterialTheme.typography.titleSmall,
                color = QuantumColors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("You can still add contacts by pasting their fingerprint below.",
                style = MaterialTheme.typography.bodySmall, color = QuantumColors.TextTertiary,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
        }

        Box(
            Modifier.fillMaxWidth()
                .glassmorphism(cornerRadius = 20, overlayAlpha = 0.10f)
                .background(QuantumColors.GlassWhite08, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, null, tint = QuantumColors.Primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Paste Fingerprint", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = QuantumColors.TextPrimary)
                }
                Text("Ask your contact to share their 64-character Text Fingerprint from their Profile screen.",
                    style = MaterialTheme.typography.bodySmall, color = QuantumColors.TextTertiary)

                // Contact name field
                OutlinedTextField(
                    value = contactName,
                    onValueChange = onContactNameChange,
                    label = { Text("Contact Name", color = QuantumColors.TextTertiary) },
                    placeholder = { Text("e.g. John", color = QuantumColors.TextTertiary) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = QuantumColors.TextTertiary, modifier = Modifier.size(18.dp)) },
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

                // Fingerprint field
                OutlinedTextField(
                    value = fingerprint,
                    onValueChange = { if (it.length <= 64) onFingerprintChange(it.lowercase()) },
                    placeholder = { Text("e.g. a1b2c3d4e5f6…", color = QuantumColors.TextTertiary) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                    ),
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

                // Character count
                Text(
                    "${fingerprint.length}/64",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fingerprint.length == 64) QuantumColors.Success else QuantumColors.TextTertiary,
                    modifier = Modifier.align(Alignment.End)
                )

                Button(
                    onClick = onSubmit,
                    enabled = fingerprint.length == 64 && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumColors.Primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (isLoading) "Looking up…" else "Add Contact", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status card (loading / error / self-add feedback)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(uiState: AddContactUiState, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = uiState is AddContactUiState.Loading ||
                  uiState is AddContactUiState.Error ||
                  uiState is AddContactUiState.SelfAdd,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        val (icon, message, color) = when (uiState) {
            is AddContactUiState.Loading -> Triple(Icons.Default.Search, "Looking up identity…", QuantumColors.Primary)
            is AddContactUiState.Error   -> Triple(Icons.Default.ErrorOutline, uiState.message, QuantumColors.Error)
            is AddContactUiState.SelfAdd -> Triple(Icons.Default.PersonOff, "That's your own fingerprint!", QuantumColors.Warning)
            else -> Triple(Icons.Default.Info, "", QuantumColors.TextTertiary)
        }

        Box(
            Modifier.fillMaxWidth()
                .glassmorphism(cornerRadius = 16, overlayAlpha = 0.15f)
                .background(QuantumColors.Surface.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { if (uiState !is AddContactUiState.Loading) onDismiss() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState is AddContactUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = color, strokeWidth = 2.dp)
                } else {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = QuantumColors.TextPrimary,
                    modifier = Modifier.weight(1f))
                if (uiState !is AddContactUiState.Loading) {
                    Icon(Icons.Default.Close, "Dismiss", tint = QuantumColors.TextTertiary,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

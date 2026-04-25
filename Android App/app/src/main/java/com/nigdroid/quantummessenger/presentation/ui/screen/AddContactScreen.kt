package com.nigdroid.quantummessenger.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.glassmorphism

@Composable
fun AddContactScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(100.dp)
                    .glassmorphism(cornerRadius = 50, overlayAlpha = 0.15f, usePrimaryTint = true)
                    .background(
                        Brush.linearGradient(listOf(
                            QuantumColors.Primary.copy(alpha = 0.5f),
                            QuantumColors.Accent.copy(alpha = 0.3f))),
                        CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Add Contact", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = QuantumColors.TextPrimary)
            Spacer(Modifier.height(10.dp))
            Text("Scan a QR code or enter a\nText Fingerprint to add a contact.",
                style = MaterialTheme.typography.bodyMedium, color = QuantumColors.TextTertiary,
                textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
    }
}

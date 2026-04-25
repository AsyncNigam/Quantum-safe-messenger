package com.nigdroid.quantummessenger.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme

// ─────────────────────────────────────────────────────────────────────────────
// Tab model
// ─────────────────────────────────────────────────────────────────────────────

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Chats(
        label          = "Chats",
        selectedIcon   = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    ),
    AddContact(
        label          = "Add Contact",
        selectedIcon   = Icons.Filled.PersonAdd,
        unselectedIcon = Icons.Outlined.PersonAddAlt
    ),
    Profile(
        label          = "Profile",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom Navigation Bar — frosted glass, floating
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuantumBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // ── Frosted glass container ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(22.dp))
                // Layered glass effect
                .drawBehind {
                    // Base dark glass
                    drawRoundRect(
                        color       = Color(0xCC0D0B18),
                        cornerRadius = CornerRadius(22.dp.toPx())
                    )
                    // Top highlight edge
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY   = size.height * 0.4f
                        ),
                        cornerRadius = CornerRadius(22.dp.toPx())
                    )
                    // Border glow
                    drawRoundRect(
                        color       = Color.White.copy(alpha = 0.10f),
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style       = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx()
                        )
                    )
                }
                .blur(0.5.dp)
        ) {
            Row(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                BottomNavTab.entries.forEach { tab ->
                    NavItem(
                        tab        = tab,
                        isSelected = tab == currentTab,
                        onClick    = { onTabSelected(tab) },
                        modifier   = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual nav item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor: Color by animateColorAsState(
        targetValue   = if (isSelected) QuantumColors.Primary else QuantumColors.TextTertiary,
        animationSpec = tween(250),
        label         = "iconColor"
    )
    val labelAlpha: Float by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.5f,
        animationSpec = tween(250),
        label         = "labelAlpha"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Active indicator pill ────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .background(
                            color = QuantumColors.Primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
            Icon(
                imageVector        = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint               = iconColor,
                modifier           = Modifier.size(22.dp)
            )
        }

        Text(
            text       = tab.label,
            style      = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color      = iconColor.copy(alpha = labelAlpha)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(backgroundColor = 0xFF08070E, showBackground = true, widthDp = 390)
@Composable
private fun PreviewBottomNav() {
    QuantumMessengerTheme {
        QuantumBottomNavBar(
            currentTab    = BottomNavTab.Chats,
            onTabSelected = {}
        )
    }
}

package com.nigdroid.quantummessenger.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme
import kotlin.math.roundToInt


enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Chats(
        label          = "Home",
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    AddContact(
        label          = "Add",
        selectedIcon   = Icons.Filled.PersonAdd,
        unselectedIcon = Icons.Outlined.PersonAddAlt
    ),
    Profile(
        label          = "Profile",
        selectedIcon   = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    )
}


@Composable
fun QuantumBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current

    val tabPositions = remember { mutableStateMapOf<BottomNavTab, Pair<Float, Float>>() }

    val accentColor = if (isDark) Color(0xFFBF7030) else Color(0xFFC05878)
    val indicatorX by animateFloatAsState(
        targetValue   = tabPositions[currentTab]?.first ?: 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label         = "indicatorX"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue   = tabPositions[currentTab]?.second ?: 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label         = "indicatorWidth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(32.dp))
            .navigationBarsPadding()
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.65f)
                .height(20.dp)
                .clip(RoundedCornerShape(50))
                .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                            accentColor.copy(alpha = if (isDark) 0.22f else 0.16f),
                            accentColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .drawBehind {
                    drawRoundRect(
                        color        = if (isDark)
                            Color(0xFF1A1208).copy(alpha = 0.85f)
                        else
                            Color(0xFFFFFAF8).copy(alpha = 0.80f),
                        cornerRadius = CornerRadius(32.dp.toPx())
                    )
                    drawRoundRect(
                        color        = if (isDark)
                            Color(0xFFBF7030).copy(alpha = 0.06f)
                        else
                            Color(0xFFC05878).copy(alpha = 0.06f),
                        cornerRadius = CornerRadius(32.dp.toPx())
                    )
                    drawRoundRect(
                        brush        = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.06f else 0.25f),
                                Color.Transparent
                            ),
                            endY = size.height * 0.4f
                        ),
                        cornerRadius = CornerRadius(32.dp.toPx())
                    )
                    drawRoundRect(
                        color        = if (isDark)
                            Color(0xFFD4904A).copy(alpha = 0.14f)
                        else
                            Color(0xFFC05878).copy(alpha = 0.14f),
                        cornerRadius = CornerRadius(32.dp.toPx()),
                        style        = Stroke(width = 0.7.dp.toPx())
                    )

                    if (indicatorWidth > 0f) {
                        val pillHeight = 42.dp.toPx()
                        val pillY      = (size.height - pillHeight) / 2f
                        val pillCorner = CornerRadius(21.dp.toPx())

                        // Minimum edge inset to clear the 32dp rounded corners
                        val edgeInset = 10.dp.toPx()

                        // Clamp indicator pill position so it never overlaps rounded edges
                        val clampedX     = indicatorX.coerceAtLeast(edgeInset)
                        val clampedEnd   = (indicatorX + indicatorWidth).coerceAtMost(size.width - edgeInset)
                        val clampedWidth = (clampedEnd - clampedX).coerceAtLeast(0f)

                        // Outer glow — slightly larger, also clamped
                        val glowInset = edgeInset - 2.dp.toPx()
                        val glowX     = (clampedX - 4.dp.toPx()).coerceAtLeast(glowInset)
                        val glowEnd   = (clampedEnd + 4.dp.toPx()).coerceAtMost(size.width - glowInset)
                        val glowWidth = (glowEnd - glowX).coerceAtLeast(0f)

                        drawRoundRect(
                            color        = accentColor.copy(alpha = if (isDark) 0.12f else 0.10f),
                            topLeft      = Offset(glowX, pillY - 3.dp.toPx()),
                            size         = Size(glowWidth, pillHeight + 6.dp.toPx()),
                            cornerRadius = CornerRadius(24.dp.toPx())
                        )

                        drawRoundRect(
                            brush        = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (isDark) 0.18f else 0.14f),
                                    accentColor.copy(alpha = if (isDark) 0.10f else 0.08f)
                                ),
                                start = Offset(clampedX, pillY),
                                end   = Offset(clampedEnd, pillY + pillHeight)
                            ),
                            topLeft      = Offset(clampedX, pillY),
                            size         = Size(clampedWidth, pillHeight),
                            cornerRadius = pillCorner
                        )

                        // Indicator border
                        drawRoundRect(
                            color        = accentColor.copy(alpha = if (isDark) 0.22f else 0.18f),
                            topLeft      = Offset(clampedX, pillY),
                            size         = Size(clampedWidth, pillHeight),
                            cornerRadius = pillCorner,
                            style        = Stroke(width = 0.6.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                NavItem(
                    tab          = tab,
                    isSelected   = tab == currentTab,
                    isDark       = isDark,
                    accentColor  = accentColor,
                    onClick      = {
                        if (tab != currentTab) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onTabSelected(tab)
                    },
                    onPositioned = { x, width -> tabPositions[tab] = Pair(x, width) },
                    modifier     = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
private fun NavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    isDark: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onPositioned: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val inactiveIconColor = if (isDark)
        Color(0xFFAA9880).copy(alpha = 0.45f)
    else
        Color(0xFF281418).copy(alpha = 0.35f)

    val iconColor by animateColorAsState(
        targetValue   = if (isSelected) accentColor else inactiveIconColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "iconColor"
    )

    val iconScale by animateFloatAsState(
        targetValue   = if (isSelected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label         = "iconScale"
    )

    val dotScale by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label         = "dotScale"
    )

    val labelAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label         = "labelAlpha"
    )
    val labelOffsetX by animateFloatAsState(
        targetValue   = if (isSelected) 0f else -10f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label         = "labelOffsetX"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 6.dp)
            .onGloballyPositioned { coordinates ->
                val x     = coordinates.positionInParent().x
                val width = coordinates.size.width.toFloat()
                onPositioned(x, width)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier         = Modifier
                        .size(22.dp)
                        // Only apply scale to non-AddContact tabs
                        .scale(if (tab == BottomNavTab.AddContact) 1f else iconScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Unselected icon — fades out
                    Icon(
                        imageVector        = tab.unselectedIcon,
                        contentDescription = null,
                        tint               = inactiveIconColor.copy(alpha = inactiveIconColor.alpha * (1f - labelAlpha)),
                        modifier           = Modifier.fillMaxSize()
                    )
                    Icon(
                        imageVector        = tab.selectedIcon,
                        contentDescription = tab.label,
                        tint               = accentColor.copy(alpha = labelAlpha),
                        modifier           = Modifier.fillMaxSize()
                    )
                }

                AnimatedVisibility(
                    visible = isSelected,
                    enter   = expandHorizontally(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        expandFrom    = Alignment.Start
                    ) + fadeIn(tween(260)),
                    exit    = shrinkHorizontally(
                        animationSpec = tween(200),
                        shrinkTowards = Alignment.Start
                    ) + fadeOut(tween(180))
                ) {
                    Row {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text     = tab.label,
                            style    = MaterialTheme.typography.labelLarge.copy(
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color    = accentColor.copy(alpha = labelAlpha),
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                alpha        = labelAlpha
                                translationX = labelOffsetX
                            }
                        )
                    }
                }
            }

            // Active dot
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .scale(dotScale)
                    .background(
                        color = accentColor.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
        }
    }
}


@Preview(backgroundColor = 0xFF0E0A04, showBackground = true, widthDp = 390, name = "Dark — Chats")
@Composable
fun PreviewDarkChats() {
    QuantumMessengerTheme(darkTheme = true) {
        QuantumBottomNavBar(currentTab = BottomNavTab.Chats, onTabSelected = {}, isDark = true)
    }
}

@Preview(backgroundColor = 0xFF0E0A04, showBackground = true, widthDp = 390, name = "Dark — Add")
@Composable
fun PreviewDarkAdd() {
    QuantumMessengerTheme(darkTheme = true) {
        QuantumBottomNavBar(currentTab = BottomNavTab.AddContact, onTabSelected = {}, isDark = true)
    }
}

@Preview(backgroundColor = 0xFF0E0A04, showBackground = true, widthDp = 390, name = "Dark — Profile")
@Composable
fun PreviewDarkProfile() {
    QuantumMessengerTheme(darkTheme = true) {
        QuantumBottomNavBar(currentTab = BottomNavTab.Profile, onTabSelected = {}, isDark = true)
    }
}

@Preview(backgroundColor = 0xFFF0D4CC, showBackground = true, widthDp = 390, name = "Light")
@Composable
fun PreviewLight() {
    QuantumMessengerTheme(darkTheme = false) {
        QuantumBottomNavBar(currentTab = BottomNavTab.Chats, onTabSelected = {}, isDark = false)
    }
}
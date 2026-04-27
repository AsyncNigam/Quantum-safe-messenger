package com.nigdroid.quantummessenger.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumMessengerTheme

// ─── Tab model ───────────────────────────────────────────────────────────────

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

// ─── Bottom Nav Bar ───────────────────────────────────────────────────────────

@Composable
fun QuantumBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .drawBehind {
                    // Warm tinted glass base
                    drawRoundRect(
                        color = if (isDark)
                            Color(0xFFBF7030).copy(alpha = 0.08f)   // amber-tinted glass for dark
                        else
                            Color(0xFFC05878).copy(alpha = 0.10f),  // rose-tinted glass for light
                        cornerRadius = CornerRadius(28.dp.toPx())
                    )
                    // Glass border
                    drawRoundRect(
                        color = if (isDark)
                            Color(0xFFD4904A).copy(alpha = 0.18f)   // amber border
                        else
                            Color(0xFFC05878).copy(alpha = 0.22f),  // rose border
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                NavItem(
                    tab        = tab,
                    isSelected = tab == currentTab,
                    isDark     = isDark,
                    onClick    = { onTabSelected(tab) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Individual nav item ──────────────────────────────────────────────────────

@Composable
private fun NavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIconColor = if (isDark) Color(0xFFBF7030) else Color(0xFFC05878)
    val inactiveIconColor = if (isDark)
        Color(0xFFAA9880).copy(alpha = 0.40f)
    else
        Color(0xFF281418).copy(alpha = 0.30f)

    val iconColor by animateColorAsState(
        targetValue   = if (isSelected) activeIconColor else inactiveIconColor,
        // FIXED: Use spring instead of tween — prevents color flash/flip
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "iconColor"
    )

    val pillAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        // FIXED: spring prevents the abrupt jump that caused the "flip" feel
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "pillAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Active pill background — animates smoothly in/out
        if (pillAlpha > 0f) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .fillMaxWidth(0.88f)
                    .background(
                        color = activeIconColor.copy(alpha = 0.15f * pillAlpha),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            // FIXED: Icon switching without graphicsLayer scale to prevent flip.
            // Using Crossfade ensures no mirroring artifact on PersonAdd icon.
            Crossfade(
                targetState   = isSelected,
                animationSpec = tween(200),
                label         = "iconSwitch_${tab.name}"
            ) { selected ->
                Icon(
                    imageVector        = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                    tint               = iconColor,
                    modifier           = Modifier.size(20.dp)
                )
            }

            // Sliding label — only visible when selected
            AnimatedVisibility(
                visible = isSelected,
                enter   = expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(200, delayMillis = 60)),
                exit    = shrinkHorizontally(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(140))
            ) {
                Text(
                    text  = tab.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color    = activeIconColor,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(backgroundColor = 0xFF0E0A04, showBackground = true, widthDp = 390, name = "Dark")
@Composable
fun PreviewDark() {
    QuantumMessengerTheme(darkTheme = true) {
        QuantumBottomNavBar(currentTab = BottomNavTab.Chats, onTabSelected = {}, isDark = true)
    }
}

@Preview(backgroundColor = 0xFFF0D4CC, showBackground = true, widthDp = 390, name = "Light")
@Composable
fun PreviewLight() {
    QuantumMessengerTheme(darkTheme = false) {
        QuantumBottomNavBar(currentTab = BottomNavTab.Chats, onTabSelected = {}, isDark = false)
    }
}
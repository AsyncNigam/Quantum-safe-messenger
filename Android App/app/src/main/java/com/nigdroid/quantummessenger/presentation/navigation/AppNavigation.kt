package com.nigdroid.quantummessenger.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.nigdroid.quantummessenger.presentation.ui.components.BottomNavTab
import com.nigdroid.quantummessenger.presentation.ui.components.QuantumBottomNavBar
import com.nigdroid.quantummessenger.presentation.ui.screen.AddContactScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.ChatScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.auth.AuthScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.home.HomeScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.profile.ProfileScreen
import com.nigdroid.quantummessenger.presentation.ui.theme.QuantumColors

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Any,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTab = when {
        currentDestination?.hasRoute<HomeRoute>()       == true -> BottomNavTab.Chats
        currentDestination?.hasRoute<AddContactRoute>() == true -> BottomNavTab.AddContact
        currentDestination?.hasRoute<ProfileRoute>()    == true -> BottomNavTab.Profile
        else -> null
    }

    val showBottomNav = currentTab != null

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Global Offline Banner ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = isOffline,
            enter   = slideInVertically(
                initialOffsetY = { -it },
                animationSpec  = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(300)),
            exit    = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250)
            ) + fadeOut(tween(200))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFB71C1C),
                                Color(0xFFD32F2F)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = "No Internet",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No Internet Connection",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // ── Main App Content ──────────────────────────────────────────────────
        Scaffold(
            containerColor       = Color.Transparent,
            contentWindowInsets  = WindowInsets(0.dp),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomNav,
                    enter   = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec  = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        )
                    ) + fadeIn(tween(220)),
                    exit    = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(200)
                    ) + fadeOut(tween(160))
                ) {
                    QuantumBottomNavBar(
                        currentTab    = currentTab ?: BottomNavTab.Chats,
                        onTabSelected = { tab ->
                            val route: Any = when (tab) {
                                BottomNavTab.Chats      -> HomeRoute
                                BottomNavTab.AddContact -> AddContactRoute
                                BottomNavTab.Profile    -> ProfileRoute
                            }
                            navController.navigate(route) {
                                popUpTo(HomeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController       = navController,
                startDestination    = startDestination,
                modifier            = modifier.padding(padding),
                enterTransition     = { fadeIn(tween(260)) },
                exitTransition      = { fadeOut(tween(200)) },
                popEnterTransition  = { fadeIn(tween(260)) },
                popExitTransition   = { fadeOut(tween(200)) }
            ) {

                // ── Auth ──────────────────────────────────────────────────────────
                composable<AuthRoute>(
                    enterTransition = { fadeIn(tween(500)) },
                    exitTransition  = {
                        fadeOut(tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                    }
                ) {
                    AuthScreen(
                        onAuthSuccess = {
                            navController.navigate(HomeRoute) {
                                popUpTo(AuthRoute) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Home / Chats ──────────────────────────────────────────────────
                composable<HomeRoute>(
                    enterTransition    = { fadeIn(tween(260)) },
                    exitTransition     = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(280)) },
                    popExitTransition  = { fadeOut(tween(200)) }
                ) {
                    HomeScreen(
                        onChatClick    = { userId -> navController.navigate(ChatRoute(userId)) },
                        onNewChatClick = { navController.navigate(AddContactRoute) }
                    )
                }

                // ── Add Contact ───────────────────────────────────────────────────
                composable<AddContactRoute>(
                    enterTransition    = { fadeIn(tween(260)) },
                    exitTransition     = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(260)) },
                    popExitTransition  = { fadeOut(tween(200)) }
                ) {
                    AddContactScreen(
                        onContactAdded = { fingerprint ->
                            navController.navigate(ChatRoute(fingerprint)) {
                                popUpTo(HomeRoute)
                            }
                        }
                    )
                }

                composable<ProfileRoute>(
                    enterTransition    = { fadeIn(tween(260)) },
                    exitTransition     = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(260)) },
                    popExitTransition  = { fadeOut(tween(200)) }
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ProfileScreen(
                        onAccountCleared = {
                            // After account deletion the Hilt singleton DAOs hold
                            // references to the now-closed/deleted Room database.
                            // The only reliable way to get a clean slate is to
                            // restart the process.  This is the standard pattern
                            // used by Signal, WhatsApp, Telegram, etc.
                            val intent = context.packageManager
                                .getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                            context.startActivity(intent)
                            (context as? android.app.Activity)?.finishAffinity()
                            // Exit code 0 = clean shutdown, not a crash.
                            // Android will immediately re-launch the app via the
                            // pending intent above.
                            kotlin.system.exitProcess(0)
                        }
                    )
                }

                composable<ChatRoute>(
                    enterTransition = {
                        slideIntoContainer(
                            towards       = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness    = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards       = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness    = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(tween(240))
                    },

                    popEnterTransition = {
                        fadeIn(tween(260))
                    },

                    popExitTransition = {
                        slideOutOfContainer(
                            towards       = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness    = Spring.StiffnessLow   // softer exit on back
                            )
                        ) + fadeOut(tween(260))
                    }
                ) { backStackEntry ->
                    val chatRoute: ChatRoute = backStackEntry.toRoute()
                    ChatScreen(
                        participantId = chatRoute.userId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
package com.nigdroid.quantummessenger.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Any,
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
                ProfileScreen()
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
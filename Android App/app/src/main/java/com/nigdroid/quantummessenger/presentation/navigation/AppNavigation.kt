package com.nigdroid.quantummessenger.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/**
 * AppNavigation — type-safe navigation with a floating glassmorphism bottom nav.
 *
 * The bottom nav is shown on the three main tabs (Chats, AddContact, Profile)
 * and hidden on Auth and Chat screens.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine which tab is active (null if not on a tab screen)
    val currentTab = when {
        currentDestination?.hasRoute<HomeRoute>() == true       -> BottomNavTab.Chats
        currentDestination?.hasRoute<AddContactRoute>() == true -> BottomNavTab.AddContact
        currentDestination?.hasRoute<ProfileRoute>() == true    -> BottomNavTab.Profile
        else -> null
    }

    // Show bottom nav only on tab screens
    val showBottomNav = currentTab != null

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomNav) {
                QuantumBottomNavBar(
                    currentTab    = currentTab ?: BottomNavTab.Chats,
                    onTabSelected = { tab ->
                        val route: Any = when (tab) {
                            BottomNavTab.Chats      -> HomeRoute
                            BottomNavTab.AddContact -> AddContactRoute
                            BottomNavTab.Profile    -> ProfileRoute
                        }
                        navController.navigate(route) {
                            // Pop up to the start destination to avoid stacking
                            popUpTo(HomeRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = modifier.padding(padding)
        ) {
            // ── Auth Screen ──────────────────────────────────────────────
            composable<AuthRoute>(
                enterTransition = { fadeIn(tween(700)) },
                exitTransition  = { fadeOut(tween(700)) }
            ) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(HomeRoute) {
                            popUpTo(AuthRoute) { inclusive = true }
                        }
                    }
                )
            }

            // ── Home / Chats ─────────────────────────────────────────────
            composable<HomeRoute>(
                enterTransition    = { fadeIn(tween(300)) },
                exitTransition     = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition  = { fadeOut(tween(200)) }
            ) {
                HomeScreen(
                    onChatClick    = { userId -> navController.navigate(ChatRoute(userId)) },
                    onNewChatClick = { navController.navigate(AddContactRoute) }
                )
            }

            // ── Add Contact ──────────────────────────────────────────────
            composable<AddContactRoute>(
                enterTransition    = { fadeIn(tween(300)) },
                exitTransition     = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) },
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

            // ── Profile ──────────────────────────────────────────────────
            composable<ProfileRoute>(
                enterTransition    = { fadeIn(tween(300)) },
                exitTransition     = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition  = { fadeOut(tween(200)) }
            ) {
                ProfileScreen()
            }

            // ── Chat Screen ──────────────────────────────────────────────
            composable<ChatRoute>(
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(500)) + fadeIn()
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(500)) + fadeOut()
                }
            ) { backStackEntry ->
                val chatRoute: ChatRoute = backStackEntry.toRoute()
                ChatScreen(participantId = chatRoute.userId)
            }
        }
    }
}

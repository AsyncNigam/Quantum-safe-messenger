package com.nigdroid.quantummessenger.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nigdroid.quantummessenger.presentation.ui.screen.ChatScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.auth.AuthScreen
import com.nigdroid.quantummessenger.presentation.ui.screen.home.HomeScreen

/**
 * AppNavigation handles the type-safe navigation between screens.
 * Includes premium transitions matching the Glassmorphism aesthetic.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth Screen
        composable<AuthRoute>(
            enterTransition = { fadeIn(animationSpec = tween(700)) },
            exitTransition = { fadeOut(animationSpec = tween(700)) }
        ) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                }
            )
        }

        // Home/Inbox Screen
        composable<HomeRoute>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeIn()
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ) + fadeOut()
            },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            HomeScreen(
                onChatClick = { userId ->
                    navController.navigate(ChatRoute(userId))
                },
                onNewChatClick = {
                    // Navigate to Contact Discovery or similar
                }
            )
        }

        // Chat Screen
        composable<ChatRoute>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(500)
                ) + fadeIn()
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(500)
                ) + fadeOut()
            }
        ) { backStackEntry ->
            val chatRoute: ChatRoute = backStackEntry.toRoute()
            ChatScreen(participantId = chatRoute.userId)
        }
    }
}

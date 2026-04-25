package com.nigdroid.quantummessenger.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthRoute

@Serializable
object HomeRoute

@Serializable
object AddContactRoute

@Serializable
object ProfileRoute

@Serializable
data class ChatRoute(val userId: String)

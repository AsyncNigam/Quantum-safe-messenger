package com.nigdroid.quantummessenger.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthRoute

@Serializable
object HomeRoute

@Serializable
data class ChatRoute(val userId: String)

package com.nigdroid.quantummessenger.domain.di

import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import com.nigdroid.quantummessenger.domain.usecase.GetChatHistoryUseCase
import com.nigdroid.quantummessenger.domain.usecase.ReceiveMessageUseCase
import com.nigdroid.quantummessenger.domain.usecase.SendMessageUseCase
import com.nigdroid.quantummessenger.network.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing domain use cases.
 * Use cases are singletons since they don't hold mutable state.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideSendMessageUseCase(
        chatRepository: ChatRepository,
        webSocketManager: WebSocketManager
    ): SendMessageUseCase {
        return SendMessageUseCase(chatRepository, webSocketManager)
    }

    @Provides
    @Singleton
    fun provideReceiveMessageUseCase(
        chatRepository: ChatRepository,
        webSocketManager: WebSocketManager
    ): ReceiveMessageUseCase {
        return ReceiveMessageUseCase(chatRepository, webSocketManager)
    }

    @Provides
    @Singleton
    fun provideGetChatHistoryUseCase(
        chatRepository: ChatRepository
    ): GetChatHistoryUseCase {
        return GetChatHistoryUseCase(chatRepository)
    }
}

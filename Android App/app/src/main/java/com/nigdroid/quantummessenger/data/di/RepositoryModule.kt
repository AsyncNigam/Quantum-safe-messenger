package com.nigdroid.quantummessenger.data.di

import com.nigdroid.quantummessenger.data.repository.ChatRepositoryImpl
import com.nigdroid.quantummessenger.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}

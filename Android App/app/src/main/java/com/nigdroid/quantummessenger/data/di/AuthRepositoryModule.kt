package com.nigdroid.quantummessenger.data.di

import com.nigdroid.quantummessenger.data.repository.AuthRepositoryImpl
import com.nigdroid.quantummessenger.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for authentication repositories
 *
 * Provides:
 * - AuthRepository interface binding
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}


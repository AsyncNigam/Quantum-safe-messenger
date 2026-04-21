package com.nigdroid.quantummessenger.network.di

import com.nigdroid.quantummessenger.network.api.AuthenticationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt DI module for authentication network services
 *
 * Provides:
 * - AuthenticationService via Retrofit
 * - Uses secure OkHttp client with certificate pinning
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthNetworkModule {

    @Provides
    @Singleton
    fun provideAuthenticationService(
        @Named("secureRetrofit") retrofit: Retrofit
    ): AuthenticationService {
        return retrofit.create(AuthenticationService::class.java)
    }
}


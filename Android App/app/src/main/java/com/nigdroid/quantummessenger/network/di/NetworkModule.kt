package com.nigdroid.quantummessenger.network.di

import com.nigdroid.quantummessenger.network.WebSocketManager
import com.nigdroid.quantummessenger.network.api.ContactApiService
import com.nigdroid.quantummessenger.network.api.MessageApiService
import com.nigdroid.quantummessenger.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideWebSocketManager(): WebSocketManager {
        return WebSocketManager()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }

    @Provides
    @Singleton
    @Named("secureRetrofit")
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideContactApiService(@Named("secureRetrofit") retrofit: Retrofit): ContactApiService {
        return retrofit.create(ContactApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageApiService(@Named("secureRetrofit") retrofit: Retrofit): MessageApiService {
        return retrofit.create(MessageApiService::class.java)
    }
}

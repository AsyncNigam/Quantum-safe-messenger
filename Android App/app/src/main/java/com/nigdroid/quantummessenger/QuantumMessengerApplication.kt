package com.nigdroid.quantummessenger

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nigdroid.quantummessenger.network.fcm.QuantumFirebaseMessagingService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Quantum Messenger.
 * Enables Hilt dependency injection, WorkManager configuration,
 * and Firebase Cloud Messaging notification channel setup.
 */
@HiltAndroidApp
class QuantumMessengerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Set up notification channel for push notifications
        QuantumFirebaseMessagingService.createNotificationChannel(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

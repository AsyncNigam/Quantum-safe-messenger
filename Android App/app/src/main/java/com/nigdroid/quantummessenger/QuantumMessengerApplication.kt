package com.nigdroid.quantummessenger

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nigdroid.quantummessenger.network.fcm.QuantumFirebaseMessagingService
import com.nigdroid.quantummessenger.util.AppLifecycleTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuantumMessengerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        AppLifecycleTracker.init()
        QuantumFirebaseMessagingService.createNotificationChannel(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

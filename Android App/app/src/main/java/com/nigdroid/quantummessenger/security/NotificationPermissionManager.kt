package com.nigdroid.quantummessenger.security

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper to request notification permission at runtime.
 * Required for Android 13+ (API 33+)
 *
 * Usage:
 *   NotificationPermissionManager.requestNotificationPermission(activity)
 */
object NotificationPermissionManager {
    private const val TAG = "NotifPermission"
    private const val REQUEST_CODE = 9001

    /**
     * Request POST_NOTIFICATIONS permission from user.
     * Safe to call on all Android versions — returns early on Android 12 and below.
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android < 13 — notification permission granted by default")
            return
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            Log.d(TAG, "✅ POST_NOTIFICATIONS permission already granted")
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            Log.d(TAG, "User previously denied — showing request again")
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            REQUEST_CODE
        )
        Log.d(TAG, "Requesting POST_NOTIFICATIONS permission…")
    }

    /**
     * Check if notification permission is granted.
     * Returns true on Android < 13 (always granted).
     */
    fun isNotificationPermissionGranted(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

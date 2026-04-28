# Firebase Push Notifications Setup Guide

## 🔴 Issue #1: Backend - Firebase Service Account Missing

### Backend Status
```
[Firebase] ⚠️ FIREBASE_SERVICE_ACCOUNT_PATH not set — push notifications disabled.
```

### Step 1: Get Firebase Service Account JSON

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **Quantum Messenger** project
3. Click **⚙️ Project Settings** (gear icon, top-left)
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key** button
6. A JSON file will download: `quantum-messenger-xxx-firebase-adminsdk-xxx.json`

### Step 2: Add Service Account to Backend

1. Copy the downloaded JSON file
2. Paste it into this exact location:
   ```
   c:\Users\spnsh\Desktop\Quantum Messenger\Backend\firebase-service-account.json
   ```
   - **File name must be exactly:** `firebase-service-account.json`
   - **Location must be in Backend folder** (same level as `docker-compose.yml`)

### Step 3: Rebuild Backend Docker Image

```powershell
cd "c:\Users\spnsh\Desktop\Quantum Messenger\Backend"
docker compose restart quantum-backend
```

Wait 15 seconds for restart, then verify:
```powershell
docker logs quantum-backend | Select-String "Firebase"
```

**Expected output:**
```
[Firebase] ✅ Initialized with service account
```

If you still see warning, wait another 10 seconds and check again.

---

## 🟢 Issue #2: Android - Permission Request ✅ FIXED

Your Android app now properly requests notification permissions on launch.

---

## Issue 2: Android - Missing Runtime Permission Request ✅ FIXED

**Files Updated:**
- ✅ Created: `NotificationPermissionManager.kt` - Permission request helper
- ✅ Updated: `MainActivity.kt` - Calls permission request on app start

The app now:
1. Requests `POST_NOTIFICATIONS` permission at app launch
2. Handles Android 13+ (API 33+) permission flow
3. Gracefully falls back on Android 12 and below

**What User Will See:**
- On first app launch: Permission prompt dialog
- User taps "Allow" → notifications enabled
- User taps "Don't Allow" → app works but no notifications

# Firebase Push Notifications - Complete Fix Guide

## 🔍 Analysis: What's Working vs What's Not

### ✅ **What's Already Implemented**
1. **Android Permission Request** - `NotificationPermissionManager.kt` exists and is called in `MainActivity.onCreate()`
2. **FCM Token Management** - `FcmTokenManager.kt` handles token syncing
3. **Firebase Messaging Service** - `QuantumFirebaseMessagingService.kt` receives messages
4. **Backend Routes** - `/api/auth/fcm-token` endpoint exists
5. **Backend FCM Service** - `FcmService.ts` sends push notifications
6. **Notification Channel** - Created in `QuantumMessengerApplication.kt`

### ⚠️ **Potential Issues (Most Likely)**

| Issue | Cause | Fix |
|-------|-------|-----|
| **No permission dialog appeared** | App was installed BEFORE permission request code | Uninstall & reinstall app |
| **Permission was denied** | User tapped "Deny" | Grant permission in Settings |
| **FCM token not syncing** | Backend Firebase not initialized | Verify service account JSON in Docker |
| **Notifications not arriving** | FCM token not registered in backend | Check backend logs |

---

## 🚀 **Step-by-Step Fix Process**

### **Step 1: Verify Backend Firebase Setup**

```powershell
# Check if backend can see the Firebase service account file
cd "c:\Users\spnsh\Desktop\Quantum Messenger\Backend"

# List files in Backend folder
Get-ChildItem | Select-Object Name

# You should see: firebase-service-account.json
```

**Expected output:**
```
firebase-service-account.json
docker-compose.yml
package.json
src/
...
```

### **Step 2: Restart Backend with Firebase**

```powershell
cd "c:\Users\spnsh\Desktop\Quantum Messenger"

# Stop the current backend
docker compose down

# Clear old containers
docker container prune -f

# Rebuild and start fresh
docker compose up --build

# Wait for startup (30 seconds), then check logs
docker logs quantum-backend | Select-String "Firebase"
```

**Expected logs:**
```
[Firebase] ✅ Initialized with service account
[Redis] ✅ Connected
[Socket.io] ✅ Listening
```

If you see warning instead:
```
[Firebase] ⚠️ FIREBASE_SERVICE_ACCOUNT_PATH not set — push notifications disabled.
```

**Fix:** Verify `.env` file has the correct path:
```
# Backend/.env should have:
FIREBASE_SERVICE_ACCOUNT_PATH=./firebase-service-account.json
```

### **Step 3: Fix Android Permission Request**

#### **Option A: If Using Android Studio**
1. Close app if running
2. **Uninstall** app from device:
   - Settings → Apps → Quantum Messenger → Uninstall
3. **Clean build:**
   - Android Studio → Build → Clean Project
   - Android Studio → Build → Rebuild Project
4. **Run app** on device again
5. **First launch** should show notification permission dialog

#### **Option B: If Using Command Line**
```powershell
# Uninstall current app
adb uninstall com.nigdroid.quantummessenger

# Clean and build
cd "c:\Users\spnsh\Desktop\Quantum Messenger\Android App"
./gradlew clean build

# Install fresh
adb install app/build/outputs/apk/debug/app-debug.apk

# App will open → permission dialog appears
```

### **Step 4: Grant Notification Permission**

When app opens after reinstall:
1. **Permission dialog appears** asking "Allow Quantum Messenger to send notifications?"
2. Tap **"Allow"** ✅
3. App proceeds normally

**If you missed the dialog:**
- Settings → Apps → Quantum Messenger → Permissions → Notifications → ON

### **Step 5: Verify FCM Token Syncs to Backend**

**On Android Device:**
```
1. Open Quantum Messenger
2. Navigate to Auth registration (if first time)
3. Complete registration
4. App proceeds to home screen
```

**Backend logs should show:**
```powershell
docker logs quantum-backend | Select-String "FCM|fcm|token" -Context 2

# Expected output:
[FCM] ✅ Push sent to 58a692… | messageId=c1234567890xyz
[Auth] 📱 FCM token registered | fingerprint=58a69203…
```

---

## 🧪 **Test Push Notifications**

### **Setup Test Scenario**

**Device 1 (Sender):**
1. Register account → Get fingerprint
2. Go to home screen

**Device 2 (Recipient):**
1. Register account → Get fingerprint
2. Close app completely (swipe from recent apps)

### **Send Message & Check Notification**

**Device 1:**
1. Start chat with Device 2's fingerprint
2. Type message: "Hello, push notification test"
3. Tap Send

**Device 2:**
1. Should see notification in status bar even though app is closed
2. Notification appears with sender's fingerprint
3. Tap notification → app opens to that chat

**Backend logs (check for success):**
```powershell
docker logs quantum-backend --tail 30 | Select-String "FCM|sent"
```

Expected:
```
[FCM] ✅ Push sent to 58a692… | messageId=abc123xyz
```

---

## 🔧 **Detailed Troubleshooting**

### **Problem: Still No Permission Dialog After Uninstall**

**Cause:** Permission request code might not be reachable

**Check code in MainActivity.kt (line ~92):**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // This line MUST be here:
    NotificationPermissionManager.requestNotificationPermission(this)  // ← CRITICAL
    
    ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
    // ...
}
```

If missing, add it immediately before `setContent {`

### **Problem: Backend Still Shows Firebase Warning After Restart**

**Check 1: File exists**
```powershell
Test-Path "c:\Users\spnsh\Desktop\Quantum Messenger\Backend\firebase-service-account.json"

# Should return: True
```

**Check 2: File is valid JSON**
```powershell
$json = Get-Content "c:\Users\spnsh\Desktop\Quantum Messenger\Backend\firebase-service-account.json" | ConvertFrom-Json
$json.project_id

# Should show: quantumsafe-494220
```

**Check 3: Docker can access file**
```powershell
# Check docker-compose.yml has volume mount
Select-String "firebase-service-account" "c:\Users\spnsh\Desktop\Quantum Messenger\Backend\docker-compose.yml"

# Should show volume mount like:
# - ./firebase-service-account.json:/app/firebase-service-account.json
```

**Check 4: Verify .env inside container**
```powershell
docker exec quantum-backend cat /app/.env | Select-String "FIREBASE"

# Should show: 
# FIREBASE_SERVICE_ACCOUNT_PATH=./firebase-service-account.json
```

### **Problem: FCM Token Not Showing in Backend Logs**

**Check Android logs:**
```powershell
# Watch Android app logs in real-time
adb logcat | Select-String "FcmTokenManager|QuantumFCM"

# Should show:
# D/FcmTokenManager: FCM token obtained (abc123def45…)
# D/FcmTokenManager: ✅ FCM token synced with backend
```

**If not syncing:**
1. Check network connectivity between app and backend
2. Verify backend is accessible: `http://backend:8000` (from app perspective)
3. Check AuthenticationService endpoint: POST `/api/auth/fcm-token`

### **Problem: Permission Granted but No Notifications**

**Check list:**
1. ✅ Backend Firebase initialized?
   ```powershell
   docker logs quantum-backend | Select-String "\[Firebase\]"
   ```

2. ✅ FCM token registered in database?
   ```powershell
   docker logs quantum-backend | Select-String "FCM token registered"
   ```

3. ✅ Recipient app has permission granted?
   - Settings → Apps → Quantum Messenger → Permissions → Notifications

4. ✅ Message being sent to correct recipient?
   - Check backend logs when message sent

5. ✅ Firebase admin SDK working?
   ```powershell
   docker logs quantum-backend | Select-String "Push|sent|firebase" -Context 2
   ```

---

## 📋 **Quick Checklist**

- [ ] Backend `firebase-service-account.json` exists
- [ ] Backend container restarted after adding JSON
- [ ] Backend logs show `[Firebase] ✅ Initialized with service account`
- [ ] Android app uninstalled and reinstalled
- [ ] Android permission dialog appeared and was granted
- [ ] Android logs show `✅ FCM token synced with backend`
- [ ] Backend logs show `📱 FCM token registered` for test user
- [ ] Test message sent and notification received

---

## 🆘 **Still Not Working?**

**Provide these logs for debugging:**

1. Backend startup logs (full):
```powershell
docker logs quantum-backend --tail 100
```

2. Android logcat during app launch:
```powershell
adb logcat -d | Select-String "QuantumFCM|FcmToken|NotifPermission" -Context 3
```

3. Backend logs when sending test message:
```powershell
docker logs quantum-backend --tail 30 | Select-String "FCM|Message|Push"
```

4. Backend database state (if you have access):
```sql
-- Check if FCM tokens are stored
SELECT fingerprint, fcm_token FROM fcm_tokens LIMIT 5;
```

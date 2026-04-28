# Push Notifications - Testing & Troubleshooting

## ✅ Summary of Fixes

### Backend (Docker)
- ⚠️ **Needs:** Firebase service account JSON file
- 📍 **Location:** `Backend/firebase-service-account.json`
- ⏱️ **After adding:** Restart container (15 sec)

### Android App  
- ✅ **Fixed:** Added runtime permission request
- 📁 **Created:** `NotificationPermissionManager.kt`
- 📝 **Updated:** `MainActivity.kt` imports & onCreate()
- 🔔 **Result:** User sees permission dialog on first launch

---

## 🧪 Complete Testing Workflow

### Prerequisites
1. Backend running with Firebase service account ✅
2. Android app rebuilt (with permission fix) ✅
3. Two test users registered
4. Both users' FCM tokens synced to backend

### Test Scenario: Send Message While Recipient Offline

**User A's Device:**
```
1. Open app
2. Start chat with User B
3. Type message: "Hello, test notification"
4. Tap Send
```

**Backend Response (Check Logs):**
```powershell
docker logs quantum-backend -f
```

Expected output:
```
[FCM] ✅ Push sent to 58a692… | messageId=c1234567890xyz
[Socket] Delivered (offline) | from=fp:b57c29ef | to=fp:58a69203
```

**User B's Device (Backgrounded/Closed):**
```
1. App is closed or backgrounded
2. Notification appears in status bar:
   - Title: 🔐 New Encrypted Message
   - Subtitle: From: 58A69203…
3. Tap notification
4. App opens to User A's chat
5. Message decrypts and displays
```

---

## 🔍 Troubleshooting

### Backend Issue: Still Shows Firebase Warning

```powershell
# Check backend logs
docker logs quantum-backend | Select-String "Firebase"

# If still warning after file placement:
# 1. Verify file location
Get-Item "c:\Users\spnsh\Desktop\Quantum Messenger\Backend\firebase-service-account.json"

# 2. Verify Docker has access to file
docker exec quantum-backend ls -la /app/firebase-service-account.json

# 3. Restart if needed
docker compose -f "c:\Users\spnsh\Desktop\Quantum Messenger\Backend\docker-compose.yml" restart quantum-backend
docker logs quantum-backend --tail 20
```

### Android Issue: No Permission Dialog

**Reason:** Permission request only happens once
**Fix:** Uninstall and reinstall app

```
1. Android Settings → Apps → Quantum Messenger → Uninstall
2. Rebuild app in Android Studio
3. Run on device
4. First launch shows permission dialog
```

### Android Issue: Permission Granted but Still No Notifications

**Possible Causes:**

1. **FCM token not synced to backend**
   ```powershell
   docker logs quantum-backend | Select-String "FCM token"
   ```
   Should show: `[Auth] 📱 FCM token registered`

2. **Backend service account still not initialized**
   ```powershell
   docker logs quantum-backend | Select-String "Firebase"
   ```
   Should show: `[Firebase] ✅ Initialized with service account`

3. **Backend database missing user token**
   - Have sender and recipient registered on backend
   - Both have valid FCM tokens in `fcm_tokens` table

### Firebase Service Account File Issues

| Issue | Solution |
|-------|----------|
| File not named exactly `firebase-service-account.json` | Rename to match exactly |
| File in wrong folder | Move to Backend root, not subfolder |
| File is corrupt | Download fresh from Firebase Console |
| Permission denied on file | Check Windows file permissions |
| Docker can't read file | Restart Docker Desktop |

---

## 📊 Verification Checklist

- [ ] Firebase service account JSON downloaded
- [ ] File placed at: `Backend/firebase-service-account.json`
- [ ] Backend container restarted
- [ ] Backend logs show: `[Firebase] ✅ Initialized`
- [ ] Android app rebuilt (new code included)
- [ ] App installed on phone/emulator
- [ ] Permission dialog appeared on first launch
- [ ] User tapped "Allow" for notifications
- [ ] Backend logs show: `[Auth] 📱 FCM token registered`
- [ ] Test message sent from User A to offline User B
- [ ] Notification appeared on User B's device
- [ ] User B tapped notification → app opened
- [ ] Message was decrypted successfully

---

## 📞 Support

If notifications still don't work after these steps:

1. **Verify backend logs:**
   ```powershell
   docker logs quantum-backend --tail 50 | Select-String -Pattern "FCM|Firebase|Error"
   ```

2. **Check Firebase Console:**
   - Project Settings → Cloud Messaging
   - Verify Server API Key exists
   - Check quota not exceeded

3. **Verify Supabase tables:**
   - `users` table has entries
   - `fcm_tokens` table has entries with valid tokens
   - Both tables have same fingerprints

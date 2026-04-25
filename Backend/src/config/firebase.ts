// ─── Firebase Admin SDK Configuration ──────────────────────────────────────────
// Initializes Firebase Admin for sending push notifications via FCM.
//
// Expects a service account key at FIREBASE_SERVICE_ACCOUNT_PATH env var,
// or uses Application Default Credentials if running on GCP.

import * as admin from 'firebase-admin';

let firebaseInitialized = false;

export function initFirebase(): void {
  if (firebaseInitialized) return;

  try {
    const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;

    if (serviceAccountPath) {
      // Use explicit service account JSON file
      const serviceAccount = require(serviceAccountPath);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
      console.log('[Firebase] ✅ Initialized with service account');
    } else {
      // Fall back to Application Default Credentials
      admin.initializeApp({
        credential: admin.credential.applicationDefault(),
      });
      console.log('[Firebase] ✅ Initialized with default credentials');
    }
    firebaseInitialized = true;
  } catch (err) {
    console.warn('[Firebase] ⚠️ Failed to initialize — push notifications disabled:', (err as Error).message);
  }
}

export function getFirebaseAdmin(): typeof admin | null {
  return firebaseInitialized ? admin : null;
}

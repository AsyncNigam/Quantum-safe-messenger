// ─── Firebase Admin SDK Configuration ──────────────────────────────────────────
// Initializes Firebase Admin for sending push notifications via FCM.
//
// Expects a Base64-encoded service account key in FIREBASE_SERVICE_ACCOUNT_BASE64 env var,
// or uses Application Default Credentials if running on GCP.

import * as admin from 'firebase-admin';

let firebaseInitialized = false;

export function initFirebase(): void {
  if (firebaseInitialized) return;

  const serviceAccountBase64 = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;

  if (!serviceAccountBase64) {
    console.warn('[Firebase] ⚠️ FIREBASE_SERVICE_ACCOUNT_BASE64 not set — push notifications disabled.');
    return;
  }

  try {
    // Decode base64 string back to JSON
    const serviceAccount = JSON.parse(
      Buffer.from(serviceAccountBase64, 'base64').toString('utf8')
    );

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    firebaseInitialized = true;
    console.log('[Firebase] ✅ Initialized with service account');
  } catch (err) {
    console.warn('[Firebase] ⚠️ Failed to initialize:', (err as Error).message);
  }
}

export function getFirebaseAdmin(): typeof admin | null {
  return firebaseInitialized ? admin : null;
}


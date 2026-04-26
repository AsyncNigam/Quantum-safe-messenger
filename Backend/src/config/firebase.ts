// ─── Firebase Admin SDK Configuration ──────────────────────────────────────────
// Initializes Firebase Admin for sending push notifications via FCM.
//
// Expects a service account key at FIREBASE_SERVICE_ACCOUNT_PATH env var,
// or uses Application Default Credentials if running on GCP.

import * as admin from 'firebase-admin';
import * as path from 'path';
import * as fs from 'fs';

let firebaseInitialized = false;

export function initFirebase(): void {
  if (firebaseInitialized) return;

  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;

  if (!serviceAccountPath) {
    console.warn('[Firebase] ⚠️ FIREBASE_SERVICE_ACCOUNT_PATH not set — push notifications disabled.');
    return;
  }

  // Resolve relative to the project root (CWD), not the module location
  const resolvedPath = path.resolve(process.cwd(), serviceAccountPath);

  if (!fs.existsSync(resolvedPath)) {
    console.warn(`[Firebase] ⚠️ Service account file not found at: ${resolvedPath}`);
    console.warn('[Firebase] ⚠️ Push notifications disabled. Download from Firebase Console → Service Accounts.');
    return;
  }

  try {
    const serviceAccount = JSON.parse(fs.readFileSync(resolvedPath, 'utf-8'));
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


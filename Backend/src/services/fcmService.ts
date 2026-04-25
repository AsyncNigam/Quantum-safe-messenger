// ─── FCM Push Notification Service ────────────────────────────────────────────
// Sends Zero-Knowledge push notifications to offline users.
// Only metadata (sender fingerprint, type) is in the push payload —
// the actual message content is NEVER sent via FCM.

import { SupabaseClient } from '@supabase/supabase-js';
import { getFirebaseAdmin } from '../config/firebase';

export class FcmService {
  constructor(private readonly db: SupabaseClient) {}

  /**
   * Store or update the FCM token for a given user fingerprint.
   */
  async upsertToken(fingerprint: string, fcmToken: string): Promise<void> {
    const { error } = await this.db
      .from('fcm_tokens')
      .upsert(
        {
          fingerprint,
          fcm_token: fcmToken,
          updated_at: new Date().toISOString(),
        },
        { onConflict: 'fingerprint' },
      );

    if (error) {
      throw new Error(`Failed to save FCM token: ${error.message}`);
    }
  }

  /**
   * Retrieve the FCM token for a given fingerprint.
   */
  async getToken(fingerprint: string): Promise<string | null> {
    const { data, error } = await this.db
      .from('fcm_tokens')
      .select('fcm_token')
      .eq('fingerprint', fingerprint)
      .maybeSingle();

    if (error) {
      console.error(`[FCM] Error fetching token: ${error.message}`);
      return null;
    }

    return data?.fcm_token ?? null;
  }

  /**
   * Send a push notification to a user.
   *
   * The payload is Zero-Knowledge compliant:
   * - Only the sender's fingerprint and notification type are included
   * - The actual message content is NEVER in the push payload
   */
  async sendPushNotification(
    recipientFingerprint: string,
    senderFingerprint: string,
    type: 'new_message' | 'contact_request' = 'new_message',
  ): Promise<boolean> {
    const admin = getFirebaseAdmin();
    if (!admin) {
      console.warn('[FCM] Firebase not initialized — skipping push notification');
      return false;
    }

    const token = await this.getToken(recipientFingerprint);
    if (!token) {
      console.log(`[FCM] No FCM token for ${recipientFingerprint.slice(0, 12)}… — user may be offline`);
      return false;
    }

    try {
      const message = {
        token,
        data: {
          type,
          senderFingerprint,
        },
        android: {
          priority: 'high' as const,
          ttl: 86400000, // 24 hours
        },
      };

      const response = await admin.messaging().send(message);
      console.log(`[FCM] ✅ Push sent to ${recipientFingerprint.slice(0, 12)}… | messageId=${response}`);
      return true;
    } catch (err: any) {
      // Handle invalid/expired tokens
      if (
        err.code === 'messaging/invalid-registration-token' ||
        err.code === 'messaging/registration-token-not-registered'
      ) {
        console.warn(`[FCM] ⚠️ Invalid token for ${recipientFingerprint.slice(0, 12)}… — removing`);
        await this.removeToken(recipientFingerprint);
      } else {
        console.error(`[FCM] ❌ Push failed: ${err.message}`);
      }
      return false;
    }
  }

  /**
   * Remove a stale FCM token.
   */
  private async removeToken(fingerprint: string): Promise<void> {
    await this.db
      .from('fcm_tokens')
      .delete()
      .eq('fingerprint', fingerprint);
  }
}

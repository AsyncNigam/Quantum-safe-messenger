import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';
import { FcmService } from '../../services/fcmService';
import { redisAvailable } from '../../config/redis';

export class SocketController {
  constructor(
    private readonly messageService: MessageService,
    private readonly fcmService: FcmService,
  ) {}

  private async isOnline(io: SocketIOServer, fingerprint: string): Promise<boolean> {
    const sockets = await io.in(fingerprint).fetchSockets();
    return sockets.length > 0;
  }

  /**
   * Binds all socket event listeners.
   *
   * NOTE: By the time this runs, `socketAuthMiddleware` has already verified
   * the fingerprint and attached `socket.data.fingerprint`. We trust that value
   * completely — no need to re-validate here.
   */
  public handleConnection = (io: SocketIOServer, socket: Socket): void => {
    // fingerprint is guaranteed by socketAuthMiddleware
    const fingerprint: string = socket.data.fingerprint;

    socket.join(fingerprint);
    console.log(`[Socket] Connected    | socket=${socket.id} | fp=${fingerprint.slice(0, 12)}…`);

    // ── Drain offline queue on connect ──────────────────────────────────────
    if (redisAvailable) {
      this.messageService.retrieveAndClearOfflineMessages(fingerprint)
        .then((buffers: Buffer[]) => {
          if (buffers.length > 0) {
            console.log(`[Socket] Draining ${buffers.length} offline message(s) → fp=${fingerprint.slice(0, 12)}…`);
            buffers.forEach((buf) => {
              try {
                const envelope = JSON.parse(buf.toString());
                socket.emit('receive_message', envelope);
              } catch (e) {
                console.warn(`[Socket] Failed to parse offline message | fp=${fingerprint.slice(0, 12)}…`);
              }
            });
          }
        })
        .catch((err: Error) =>
          console.error(`[Socket] Drain error | fp=${fingerprint.slice(0, 12)}… |`, err.message),
        );
    }

    // ── send_message ────────────────────────────────────────────────────────
    // The Android client sends: { to: "fingerprint", payload: "base64..." }
    // Payload is opaque (encrypted protobuf) — server never inspects it.
    socket.on('send_message', async (data: any) => {
      try {
        const to = data?.to as string | undefined;
        const payload = data?.payload;

        if (!to || typeof to !== 'string') {
          console.warn(`[Socket] Invalid send_message — missing 'to' | socket=${socket.id}`);
          return;
        }

        if (!payload) {
          console.warn(`[Socket] Invalid send_message — missing 'payload' | socket=${socket.id}`);
          return;
        }

        const recipientOnline = await this.isOnline(io, to);

        // Build the envelope to relay — include sender fingerprint
        const envelope = { from: fingerprint, payload, sentAt: new Date().toISOString() };

        if (recipientOnline) {
          io.to(to).emit('receive_message', envelope);
          console.log(`[Socket] Delivered (online)  | from=fp:${fingerprint.slice(0, 8)} | to=fp:${to.slice(0, 8)}`);
        } else {
          // Queue offline message in Redis (if available)
          if (redisAvailable) {
            try {
              const buf = Buffer.from(JSON.stringify(envelope));
              await this.messageService.queueOfflineMessage(to, buf);
              console.log(`[Socket] Queued   (offline)  | from=fp:${fingerprint.slice(0, 8)} | to=fp:${to.slice(0, 8)}`);
            } catch (err) {
              console.warn(`[Socket] Redis queue failed:`, (err as Error).message);
            }
          } else {
            console.log(`[Socket] Dropped  (no Redis) | from=fp:${fingerprint.slice(0, 8)} | to=fp:${to.slice(0, 8)}`);
          }

          // Send push notification (Zero-Knowledge: only sender fingerprint, no content)
          this.fcmService.sendPushNotification(to, fingerprint, 'new_message')
            .catch((err) => console.warn(`[Socket] FCM push failed:`, (err as Error).message));
        }
      } catch (err: unknown) {
        console.error(`[Socket] send_message error:`, (err as Error).message);
      }
    });

    // ── disconnect ──────────────────────────────────────────────────────────
    socket.on('disconnect', () => {
      console.log(`[Socket] Disconnected | socket=${socket.id} | fp=${fingerprint.slice(0, 12)}…`);
    });
  };
}


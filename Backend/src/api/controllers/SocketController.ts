import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';
import { FcmService } from '../../services/fcmService';

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
    this.messageService.retrieveAndClearOfflineMessages(fingerprint)
      .then((buffers: Buffer[]) => {
        if (buffers.length > 0) {
          console.log(`[Socket] Draining ${buffers.length} offline message(s) → fp=${fingerprint.slice(0, 12)}…`);
          buffers.forEach((buf) => socket.emit('receive_message', buf));
        }
      })
      .catch((err: Error) =>
        console.error(`[Socket] Drain error | fp=${fingerprint.slice(0, 12)}… |`, err.message),
      );

    // ── send_message ────────────────────────────────────────────────────────
    socket.on('send_message', async ({ to, payload }: { to: string; payload: Buffer }) => {
      try {
        if (!to || typeof to !== 'string' || !payload || !Buffer.isBuffer(payload)) {
          console.warn(`[Socket] Invalid send_message dropped | socket=${socket.id}`);
          return;
        }

        const recipientOnline = await this.isOnline(io, to);

        if (recipientOnline) {
          io.to(to).emit('receive_message', payload);
          console.log(`[Socket] Delivered (online)  | from=fp:${fingerprint.slice(0, 8)} | to=fp:${to.slice(0, 8)}`);
        } else {
          // Queue message for later delivery
          await this.messageService.queueOfflineMessage(to, payload);
          console.log(`[Socket] Queued   (offline)  | from=fp:${fingerprint.slice(0, 8)} | to=fp:${to.slice(0, 8)}`);

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


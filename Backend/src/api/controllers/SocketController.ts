import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';
import { FcmService } from '../../services/fcmService';
import { UserRepository } from '../../repositories/UserRepository';
import { randomUUID } from 'crypto';

export class SocketController {
  constructor(
    private readonly messageService: MessageService,
    private readonly fcmService: FcmService,
    private readonly userRepo: UserRepository,
  ) {}

  private async isOnline(io: SocketIOServer, fingerprint: string): Promise<boolean> {
    const sockets = await io.in(fingerprint).fetchSockets();
    return sockets.length > 0;
  }

  public handleConnection = (io: SocketIOServer, socket: Socket): void => {
    const fingerprint: string = socket.data.fingerprint;

    socket.join(fingerprint);
    console.log(`[Socket] Connected    | socket=${socket.id} | fp=${fingerprint.slice(0, 12)}…`);

    this.drainOfflineQueue(fingerprint, socket);

    socket.on('send_message', async (data: any) => {
      try {
        const to = data?.to as string | undefined;
        const payload = data?.payload;

        if (!to || typeof to !== 'string') return;
        if (!payload) return;

        const recipient = await this.userRepo.findByFingerprint(to);
        if (!recipient) {
          socket.emit('user_not_found', { fingerprint: to });
          return;
        }
        if (recipient.deletedAt) {
          socket.emit('user_deleted', { fingerprint: to });
          return;
        }

        const messageId = randomUUID().replace(/-/g, '').slice(0, 16);

        const recipientOnline = await this.isOnline(io, to);

        const envelope = {
          from: fingerprint,
          payload,
          sentAt: new Date().toISOString(),
          messageId,
        };

        // ── Belt-and-suspenders: ALWAYS queue to offline storage ──
        // This guarantees the message survives even if the live delivery
        // silently fails (e.g. stale WebSocket after Render cold start).
        // The Android app has deduplication, so duplicates are harmless.
        try {
          const buf = Buffer.from(JSON.stringify(envelope));
          await this.messageService.queueOfflineMessage(to, buf);
        } catch (err) {
          console.warn(`[Socket] Queue offline message failed:`, (err as Error).message);
        }

        // ── Attempt live delivery if recipient appears online ──
        if (recipientOnline) {
          io.to(to).emit('receive_message', envelope);
        }

        // ── Always send FCM push when recipient is NOT online ──
        // Even if they ARE online, the socket may be stale, so we send
        // the push as a fallback wake-up signal.
        if (!recipientOnline) {
          this.fcmService.sendPushNotification(to, fingerprint, 'new_message')
            .catch((err) => console.warn(`[Socket] FCM push failed:`, (err as Error).message));
        }

      } catch (err: unknown) {
        console.error(`[Socket] send_message error:`, (err as Error).message);
      }
    });

    socket.on('message_ack', async (data: any) => {
      const ackId = data?.messageUuid || data?.messageId;
      if (ackId) {
        console.log(`[Socket] ACK received | fp=${fingerprint.slice(0, 8)} | id=${ackId}`);
        try {
          await this.messageService.deleteOfflineMessage(fingerprint, ackId);
        } catch (err) {
          console.warn(`[Socket] Failed to delete acknowledged message:`, (err as Error).message);
        }
      }
    });

    socket.on('disconnect', () => {
      console.log(`[Socket] Disconnected | socket=${socket.id} | fp=${fingerprint.slice(0, 12)}…`);
    });
  };

  private drainOfflineQueue(fingerprint: string, socket: Socket): void {
    this.messageService.retrieveOfflineMessages(fingerprint)
      .then((buffers: Buffer[]) => {
        if (buffers.length > 0) {
          console.log(`[Socket] Draining ${buffers.length} offline message(s) → fp=${fingerprint.slice(0, 12)}…`);
          for (const buf of buffers) {
            try {
              const envelope = JSON.parse(buf.toString());
              socket.emit('receive_message', envelope);
            } catch (e) {
              console.warn(`[Socket] Failed to parse offline message:`, (e as Error).message);
            }
          }
        }
      })
      .catch((err: Error) =>
        console.error(`[Socket] Drain error | fp=${fingerprint.slice(0, 12)}… |`, err.message),
      );
  }
}

import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';
import { FcmService } from '../../services/fcmService';
import { UserRepository } from '../../repositories/UserRepository';
import { redisAvailable } from '../../config/redis';
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

    if (redisAvailable) {
      this.drainOfflineQueue(fingerprint, socket);
    }

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

        if (recipientOnline) {
          io.to(to).emit('receive_message', envelope);
        }

        if (!recipientOnline && redisAvailable) {
          try {
            const buf = Buffer.from(JSON.stringify(envelope));
            await this.messageService.queueOfflineMessage(to, buf);
          } catch (err) {
            console.warn(`[Socket] Redis queue failed:`, (err as Error).message);
          }
        }

        // Always send FCM push — covers both cases:
        // 1. Recipient offline: wakes the app to show notification
        // 2. Recipient online but app killed before processing: ensures notification
        this.fcmService.sendPushNotification(to, fingerprint, 'new_message')
          .catch((err) => console.warn(`[Socket] FCM push failed:`, (err as Error).message));

      } catch (err: unknown) {
        console.error(`[Socket] send_message error:`, (err as Error).message);
      }
    });

    socket.on('message_ack', (data: any) => {
      const messageUuid = data?.messageUuid;
      if (messageUuid) {
        console.log(`[Socket] ACK received | fp=${fingerprint.slice(0, 8)} | uuid=${messageUuid}`);
      }
    });

    socket.on('disconnect', () => {
      console.log(`[Socket] Disconnected | socket=${socket.id} | fp=${fingerprint.slice(0, 12)}…`);
    });
  };

  private drainOfflineQueue(fingerprint: string, socket: Socket): void {
    this.messageService.retrieveAndClearOfflineMessages(fingerprint)
      .then((buffers: Buffer[]) => {
        if (buffers.length > 0) {
          console.log(`[Socket] Draining ${buffers.length} offline message(s) → fp=${fingerprint.slice(0, 12)}…`);
          for (const buf of buffers) {
            try {
              const envelope = JSON.parse(buf.toString());
              socket.emit('receive_message', envelope);
            } catch (e) {
              console.warn(`[Socket] Failed to parse offline message`);
            }
          }
        }
      })
      .catch((err: Error) =>
        console.error(`[Socket] Drain error | fp=${fingerprint.slice(0, 12)}… |`, err.message),
      );
  }
}

import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';

export class SocketController {
  constructor(private readonly messageService: MessageService) {}

  private async isUserConnected(io: SocketIOServer, userId: string): Promise<boolean> {
    const sockets = await io.in(userId).fetchSockets();
    return sockets.length > 0;
  }

  /**
   * Binds all socket event listeners.
   *
   * NOTE: By the time this runs, `socketAuthMiddleware` has already verified
   * the Supabase JWT and attached `socket.data.user`. We trust that value
   * completely — no need to re-validate the userId here.
   */
  public handleConnection = (io: SocketIOServer, socket: Socket): void => {
    // userId is guaranteed by socketAuthMiddleware — no query param fallback needed
    const userId: string = socket.data.user.id;

    socket.join(userId);
    console.log(`[Socket] Connected    | socket=${socket.id} | userId=${userId}`);

    // ── Drain offline queue on connect ──────────────────────────────────────
    this.messageService.retrieveAndClearOfflineMessages(userId)
      .then((buffers: Buffer[]) => {
        if (buffers.length > 0) {
          console.log(`[Socket] Draining ${buffers.length} offline message(s) → ${userId}`);
          buffers.forEach((buf) => socket.emit('receive_message', buf));
        }
      })
      .catch((err: Error) =>
        console.error(`[Socket] Drain error | userId=${userId} |`, err.message),
      );

    // ── send_message ────────────────────────────────────────────────────────
    socket.on('send_message', async ({ to, payload }: { to: string; payload: Buffer }) => {
      try {
        if (!to || typeof to !== 'string' || !payload || !Buffer.isBuffer(payload)) {
          console.warn(`[Socket] Invalid send_message dropped | socket=${socket.id}`);
          return;
        }

        const recipientOnline = await this.isUserConnected(io, to);

        if (recipientOnline) {
          io.to(to).emit('receive_message', payload);
          console.log(`[Socket] Delivered (online)  | from=${userId} | to=${to}`);
        } else {
          await this.messageService.queueOfflineMessage(to, payload);
          console.log(`[Socket] Queued   (offline)  | from=${userId} | to=${to}`);
        }
      } catch (err: unknown) {
        console.error(`[Socket] send_message error:`, (err as Error).message);
      }
    });

    // ── disconnect ──────────────────────────────────────────────────────────
    socket.on('disconnect', () => {
      console.log(`[Socket] Disconnected | socket=${socket.id} | userId=${userId}`);
    });
  };
}

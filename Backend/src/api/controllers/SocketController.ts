import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';

export class SocketController {
  constructor(private readonly messageService: MessageService) {}

  /**
   * Helper to check if a specific user has active sockets.
   */
  private async isUserConnected(io: SocketIOServer, userId: string): Promise<boolean> {
    const sockets = await io.in(userId).fetchSockets();
    return sockets.length > 0;
  }

  /**
   * Binds all socket event listeners for an authenticated/identified connection.
   */
  public handleConnection = (io: SocketIOServer, socket: Socket): void => {
    // Expecting the client to provide userId in handshake query
    const userId = socket.handshake.query.userId as string | undefined;

    if (!userId || typeof userId !== 'string' || userId.trim() === '') {
      console.warn(`[Socket] Connection rejected — missing userId. socket=${socket.id}`);
      socket.disconnect(true);
      return;
    }

    // Join a room exclusively for this userId
    socket.join(userId);
    console.log(`[Socket] Connected | socket=${socket.id} | room=${userId}`);

    // DRAIN OFFLINE MESSAGES ON CONNECT
    this.messageService.retrieveAndClearOfflineMessages(userId)
      .then((buffers: Buffer[]) => {
        if (buffers.length > 0) {
          console.log(`[Socket] Emitting ${buffers.length} offline message(s) to ${userId}`);
          buffers.forEach(buf => {
            // Emitting exactly as requested — raw binary Buffer
            socket.emit('receive_message', buf);
          });
        }
      })
      .catch((err: Error) => {
        console.error(`[Socket] Failed to drain queue for ${userId}:`, err.message);
      });

    // ── send_message ──────────────────────────────────────────────────────
    /**
     * Payload MUST be a Buffer containing the post-quantum encrypted content.
     */
    socket.on('send_message', async ({ to, payload }: { to: string; payload: Buffer }) => {
      try {
        if (!to || !payload || !Buffer.isBuffer(payload)) {
          console.warn(`[Socket] Invalid send_message payload dropped. socket=${socket.id}`);
          return;
        }

        const recipientOnline = await this.isUserConnected(io, to);

        if (recipientOnline) {
          // Emit the buffer directly via socket.io
          io.to(to).emit('receive_message', payload);
          console.log(`[Socket] Delivered (online) | from=${userId} | to=${to}`);
        } else {
          // Queue inside Redis as an unbroken Buffer
          await this.messageService.queueOfflineMessage(to, payload);
          console.log(`[Socket] Queued (offline) | from=${userId} | to=${to}`);
        }
      } catch (err: any) {
        console.error(`[Socket] send_message error:`, err.message);
      }
    });

    socket.on('disconnect', () => {
      console.log(`[Socket] Disconnected | socket=${socket.id} | room=${userId}`);
    });
  };
}

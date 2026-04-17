"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SocketController = void 0;
class SocketController {
    constructor(messageService) {
        this.messageService = messageService;
        /**
         * Binds all socket event listeners.
         *
         * NOTE: By the time this runs, `socketAuthMiddleware` has already verified
         * the Supabase JWT and attached `socket.data.user`. We trust that value
         * completely — no need to re-validate the userId here.
         */
        this.handleConnection = (io, socket) => {
            // userId is guaranteed by socketAuthMiddleware — no query param fallback needed
            const userId = socket.data.user.id;
            socket.join(userId);
            console.log(`[Socket] Connected    | socket=${socket.id} | userId=${userId}`);
            // ── Drain offline queue on connect ──────────────────────────────────────
            this.messageService.retrieveAndClearOfflineMessages(userId)
                .then((buffers) => {
                if (buffers.length > 0) {
                    console.log(`[Socket] Draining ${buffers.length} offline message(s) → ${userId}`);
                    buffers.forEach((buf) => socket.emit('receive_message', buf));
                }
            })
                .catch((err) => console.error(`[Socket] Drain error | userId=${userId} |`, err.message));
            // ── send_message ────────────────────────────────────────────────────────
            socket.on('send_message', async ({ to, payload }) => {
                try {
                    if (!to || typeof to !== 'string' || !payload || !Buffer.isBuffer(payload)) {
                        console.warn(`[Socket] Invalid send_message dropped | socket=${socket.id}`);
                        return;
                    }
                    const recipientOnline = await this.isUserConnected(io, to);
                    if (recipientOnline) {
                        io.to(to).emit('receive_message', payload);
                        console.log(`[Socket] Delivered (online)  | from=${userId} | to=${to}`);
                    }
                    else {
                        await this.messageService.queueOfflineMessage(to, payload);
                        console.log(`[Socket] Queued   (offline)  | from=${userId} | to=${to}`);
                    }
                }
                catch (err) {
                    console.error(`[Socket] send_message error:`, err.message);
                }
            });
            // ── disconnect ──────────────────────────────────────────────────────────
            socket.on('disconnect', () => {
                console.log(`[Socket] Disconnected | socket=${socket.id} | userId=${userId}`);
            });
        };
    }
    async isUserConnected(io, userId) {
        const sockets = await io.in(userId).fetchSockets();
        return sockets.length > 0;
    }
}
exports.SocketController = SocketController;
//# sourceMappingURL=SocketController.js.map
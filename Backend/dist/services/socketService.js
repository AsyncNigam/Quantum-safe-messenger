"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.registerSocketHandlers = void 0;
const offlineQueueService_1 = require("./offlineQueueService");
/** Returns true if the given userId has at least one active socket across all server instances. */
const isRecipientConnected = async (io, userId) => {
    const sockets = await io.in(userId).fetchSockets();
    return sockets.length > 0;
};
/**
 * Registers all Socket.io connection and event handlers onto the given server.
 * Call once during application bootstrap.
 *
 * ZERO-KNOWLEDGE POLICY:
 *   This server is a pure relay. It routes envelopes by userId only.
 *   The `payload` field is never read, logged, stored in parsed form,
 *   or modified. All encryption/decryption is performed client-side.
 */
const registerSocketHandlers = (io) => {
    io.on('connection', (socket) => {
        const userId = socket.handshake.query.userId;
        // Reject connections that do not supply a userId
        if (!userId || typeof userId !== 'string' || userId.trim() === '') {
            console.warn(`[Socket] Rejected — missing userId | socket=${socket.id}`);
            socket.disconnect(true);
            return;
        }
        // Each user occupies a private room named after their userId
        socket.join(userId);
        console.log(`[Socket] Connected    | socket=${socket.id} | room=${userId}`);
        // Deliver any messages that arrived while the user was offline
        offlineQueueService_1.offlineQueueService.drain(userId, socket).catch((err) => console.error(`[Socket] Drain error  | userId=${userId} |`, err.message));
        // ── send_message ────────────────────────────────────────────────────────
        socket.on('send_message', async (msg) => {
            const { to, payload } = msg;
            if (!to || typeof to !== 'string') {
                console.warn(`[Socket] send_message ignored — invalid 'to' | socket=${socket.id}`);
                return;
            }
            const sentAt = new Date().toISOString();
            // Log routing metadata only — payload is intentionally opaque
            console.log(`[Socket] send_message | from=${userId} | to=${to} | ` +
                `payloadType=${typeof payload} | ts=${sentAt}`);
            const envelope = { from: userId, payload, sentAt };
            const recipientOnline = await isRecipientConnected(io, to);
            if (recipientOnline) {
                io.to(to).emit('receive_message', envelope);
                console.log(`[Socket] Delivered (online)  | from=${userId} | to=${to}`);
            }
            else {
                await offlineQueueService_1.offlineQueueService.enqueue(to, envelope);
                console.log(`[Socket] Queued   (offline)  | from=${userId} | to=${to}`);
            }
        });
        // ── disconnect ──────────────────────────────────────────────────────────
        socket.on('disconnect', (reason) => {
            console.log(`[Socket] Disconnected | socket=${socket.id} | room=${userId} | reason=${reason}`);
        });
    });
};
exports.registerSocketHandlers = registerSocketHandlers;
//# sourceMappingURL=socketService.js.map
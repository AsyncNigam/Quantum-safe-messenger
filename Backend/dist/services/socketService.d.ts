import { Server as SocketIOServer } from 'socket.io';
/**
 * Registers all Socket.io connection and event handlers onto the given server.
 * Call once during application bootstrap.
 *
 * ZERO-KNOWLEDGE POLICY:
 *   This server is a pure relay. It routes envelopes by userId only.
 *   The `payload` field is never read, logged, stored in parsed form,
 *   or modified. All encryption/decryption is performed client-side.
 */
export declare const registerSocketHandlers: (io: SocketIOServer) => void;
//# sourceMappingURL=socketService.d.ts.map
import { Socket } from 'socket.io';
/**
 * Socket.io middleware that validates a Supabase JWT on every connection.
 *
 * The client MUST send the token in the `auth` object during handshake:
 *   socket = io('ws://localhost:3000', { auth: { token: '<supabase_jwt>' } });
 *
 * On success, the verified `User` object is attached to `socket.data.user`
 * so downstream handlers can access it without re-validating.
 *
 * On failure, the connection is rejected with a descriptive error that the
 * client can catch via:
 *   socket.on('connect_error', (err) => console.error(err.message));
 */
export declare const socketAuthMiddleware: (socket: Socket, next: (err?: Error) => void) => Promise<void>;
//# sourceMappingURL=socketAuthMiddleware.d.ts.map
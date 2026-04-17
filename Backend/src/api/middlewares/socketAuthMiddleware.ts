import { Socket } from 'socket.io';
import { supabase } from '../../config/supabase';

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
export const socketAuthMiddleware = async (
  socket: Socket,
  next: (err?: Error) => void,
): Promise<void> => {
  try {
    const token = socket.handshake.auth?.token as string | undefined;

    if (!token || typeof token !== 'string') {
      next(new Error('SOCKET_AUTH_MISSING: No authentication token provided.'));
      return;
    }

    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      next(new Error('SOCKET_AUTH_INVALID: Token is invalid or has expired.'));
      return;
    }

    // Attach the verified user to the socket — accessible in all event handlers
    socket.data.user = user;
    next();
  } catch (err) {
    next(err instanceof Error ? err : new Error('SOCKET_AUTH_ERROR: Authentication failed.'));
  }
};

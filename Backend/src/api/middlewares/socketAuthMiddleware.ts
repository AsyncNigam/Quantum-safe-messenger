import { Socket } from 'socket.io';
import { userRepository } from '../../repositories';

/**
 * Socket.io middleware — Zero-Knowledge fingerprint authentication.
 *
 * The client MUST send the textFingerprint in the `auth` object at handshake:
 *   socket = io('ws://...', { auth: { fingerprint: '<64-char-hex>' } });
 *
 * On success, `socket.data.fingerprint` is set for all downstream handlers.
 * On failure, the connection is rejected — client receives a `connect_error`.
 */
export const socketAuthMiddleware = async (
  socket: Socket,
  next: (err?: Error) => void,
): Promise<void> => {
  try {
    const fingerprint = socket.handshake.auth?.fingerprint as string | undefined;

    if (!fingerprint || typeof fingerprint !== 'string') {
      next(new Error('SOCKET_AUTH_MISSING: No fingerprint provided.'));
      return;
    }

    if (fingerprint.length !== 64 || !/^[a-f0-9]+$/.test(fingerprint)) {
      next(new Error('SOCKET_AUTH_INVALID: Fingerprint format is invalid.'));
      return;
    }

    const user = await userRepository.findByFingerprint(fingerprint);

    if (!user) {
      next(new Error('SOCKET_AUTH_UNKNOWN: Identity not registered.'));
      return;
    }

    // Attach verified fingerprint — accessible in all event handlers
    socket.data.fingerprint = user.fingerprint;
    next();
  } catch (err) {
    next(err instanceof Error ? err : new Error('SOCKET_AUTH_ERROR: Authentication failed.'));
  }
};

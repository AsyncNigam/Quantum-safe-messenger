import { Socket } from 'socket.io';
import { MessageEnvelope } from '../models/message';
export declare const offlineQueueService: {
    /**
     * Appends an opaque message envelope to the recipient's Redis list and
     * refreshes the TTL to 24 hours.
     *
     * ZERO-KNOWLEDGE: the envelope's `payload` is serialised as-is via
     * JSON.stringify without any inspection or transformation.
     */
    enqueue(recipientId: string, envelope: MessageEnvelope): Promise<void>;
    /**
     * Reads all messages from the user's offline queue, emits each to their
     * socket, then atomically deletes the queue key.
     *
     * ZERO-KNOWLEDGE: the inner `payload` of each envelope is never inspected;
     * only the envelope wrapper is parsed for routing purposes.
     */
    drain(userId: string, socket: Socket): Promise<void>;
};
//# sourceMappingURL=offlineQueueService.d.ts.map
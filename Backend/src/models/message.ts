/**
 * The opaque envelope relayed between clients.
 *
 * ZERO-KNOWLEDGE NOTE:
 *   `payload` is typed as `unknown` intentionally. The server never
 *   inspects, transforms, or decrypts this field. It is an opaque blob
 *   whose structure is known only to the communicating clients.
 */
export interface MessageEnvelope {
  from: string;
  payload: unknown;
  sentAt: string;
}

/** Shape of the data a client sends when emitting `send_message`. */
export interface SendMessagePayload {
  to: string;
  payload: unknown;
}

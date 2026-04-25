export interface IKeyBundle {
  /** SHA-256 fingerprint — the user's permanent ZK identity */
  fingerprint: string;
  x25519PublicKey: string;
  mlKemPublicKey: string;
  ed25519Signature: string;
  mlDsaSignature: string;
}

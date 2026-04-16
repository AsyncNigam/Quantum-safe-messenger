export interface IKeyBundle {
  userId: string;
  x25519PublicKey: string;
  mlKemPublicKey: string;
  ed25519Signature: string;
  mlDsaSignature: string;
}

import { z } from 'zod';
import { Request, Response, NextFunction } from 'express';
/**
 * Zod schema for the key bundle submitted to POST /keys/upload.
 *
 * Fields:
 *   x25519PublicKey  — Classical Diffie-Hellman key (32 bytes, Base64)
 *   mlKemPublicKey   — ML-KEM (Kyber) post-quantum KEM key (Base64)
 *   ed25519Signature — Classical signature over the key bundle (Base64)
 *   mlDsaSignature   — ML-DSA (Dilithium) post-quantum signature (Base64)
 */
export declare const keyBundleSchema: z.ZodObject<{
    x25519PublicKey: z.ZodString;
    mlKemPublicKey: z.ZodString;
    ed25519Signature: z.ZodString;
    mlDsaSignature: z.ZodString;
}, z.core.$strip>;
export type KeyBundleInput = z.infer<typeof keyBundleSchema>;
/**
 * Express middleware that validates req.body against the keyBundleSchema.
 * On success it replaces req.body with the sanitized, parsed object.
 * On failure it responds immediately with 400 and the exact validation issues.
 */
export declare const validateKeyBundle: (req: Request, res: Response, next: NextFunction) => void;
//# sourceMappingURL=keyBundleValidator.d.ts.map
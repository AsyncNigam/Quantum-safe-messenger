import { z } from 'zod';
import { Request, Response, NextFunction } from 'express';

/**
 * Regex for URL-safe Base64 and standard Base64 encoded strings.
 * All post-quantum public key material is expected in Base64.
 */
const base64Regex = /^[A-Za-z0-9+/\-_]+=*$/;

/**
 * Zod schema for the key bundle submitted to POST /keys/upload.
 *
 * Fields:
 *   x25519PublicKey  — Classical Diffie-Hellman key (32 bytes, Base64)
 *   mlKemPublicKey   — ML-KEM (Kyber) post-quantum KEM key (Base64)
 *   ed25519Signature — Classical signature over the key bundle (Base64)
 *   mlDsaSignature   — ML-DSA (Dilithium) post-quantum signature (Base64)
 */
export const keyBundleSchema = z.object({
  x25519PublicKey:  z.string().min(44).regex(base64Regex, 'Must be Base64 encoded'),
  mlKemPublicKey:   z.string().min(44).regex(base64Regex, 'Must be Base64 encoded'),
  ed25519Signature: z.string().min(44).regex(base64Regex, 'Must be Base64 encoded'),
  mlDsaSignature:   z.string().min(44).regex(base64Regex, 'Must be Base64 encoded'),
});

export type KeyBundleInput = z.infer<typeof keyBundleSchema>;

/**
 * Express middleware that validates req.body against the keyBundleSchema.
 * On success it replaces req.body with the sanitized, parsed object.
 * On failure it responds immediately with 400 and the exact validation issues.
 */
export const validateKeyBundle = (
  req: Request,
  res: Response,
  next: NextFunction,
): void => {
  console.log('[VALIDATOR] Received body keys:', Object.keys(req.body));
  console.log('[VALIDATOR] Body:', JSON.stringify(req.body, null, 2));
  
  const result = keyBundleSchema.safeParse(req.body);

  if (!result.success) {
    console.log('[VALIDATOR] ❌ Validation failed:');
    result.error.issues.forEach(issue => {
      console.log(`  - ${issue.path.join('.')}: ${issue.message}`);
    });
    
    res.status(400).json({
      error: 'Validation failed',
      issues: result.error.issues.map((issue) => ({
        field:   issue.path.join('.'),
        message: issue.message,
      })),
    });
    return;
  }

  console.log('[VALIDATOR] ✅ Validation passed');
  // Replace raw body with sanitized, schema-validated data
  req.body = result.data;
  next();
};

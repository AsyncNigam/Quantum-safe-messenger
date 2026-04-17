import { supabase } from '../config/supabase';
import { KeyRepository } from './KeyRepository';

/**
 * Singleton KeyRepository instance.
 * The Supabase client is injected here once and shared across the app.
 */
export const keyRepository = new KeyRepository(supabase);

// Export the class for type safety in other files
export * from './KeyRepository';

import { storeClient } from '../config/redis';
import { MessageService } from './MessageService';
import { keyService }     from './keyService';

/**
 * Singleton MessageService instance.
 * Receives the dedicated Redis store client via constructor injection.
 */
export const messageService = new MessageService(storeClient);

/**
 * Re-export keyService (already a singleton object literal).
 */
export { keyService };

// Export classes for type safety in other files
export * from './MessageService';
export * from './keyService';

import { MessageService } from './MessageService';
import { keyService } from './keyService';
/**
 * Singleton MessageService instance.
 * Receives the dedicated Redis store client via constructor injection.
 */
export declare const messageService: MessageService;
/**
 * Re-export keyService (already a singleton object literal).
 */
export { keyService };
export * from './MessageService';
export * from './keyService';
//# sourceMappingURL=index.d.ts.map
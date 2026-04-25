import { keyRepository }   from '../../repositories';
import { userRepository }  from '../../repositories';
import { messageService }  from '../../services';
import { KeyController }   from './KeyController';
import { SocketController } from './SocketController';
import { AuthController }  from './AuthController';

/** Singleton AuthController — receives UserRepository */
export const authController = new AuthController(userRepository);

/** Singleton KeyController — receives shared KeyRepository */
export const keyController = new KeyController(keyRepository);

/** Singleton SocketController — receives shared MessageService */
export const socketController = new SocketController(messageService);

// Export classes for type safety in other files
export * from './AuthController';
export * from './KeyController';
export * from './SocketController';

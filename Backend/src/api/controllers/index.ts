import { keyRepository }   from '../../repositories';
import { userRepository }  from '../../repositories';
import { messageService, fcmService }  from '../../services';
import { KeyController }   from './KeyController';
import { SocketController } from './SocketController';
import { AuthController }  from './AuthController';

/** Singleton AuthController — receives UserRepository and FcmService */
export const authController = new AuthController(userRepository, fcmService);

/** Singleton KeyController — receives shared KeyRepository */
export const keyController = new KeyController(keyRepository);

/** Singleton SocketController — receives shared MessageService and FcmService */
export const socketController = new SocketController(messageService, fcmService, userRepository);

// Export classes for type safety in other files
export * from './AuthController';
export * from './KeyController';
export * from './SocketController';

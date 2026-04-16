import { keyRepository }  from '../../repositories';
import { messageService }  from '../../services';
import { KeyController }    from './KeyController';
import { SocketController } from './SocketController';

/**
 * Singleton KeyController — receives the shared KeyRepository.
 */
export const keyController = new KeyController(keyRepository);

/**
 * Singleton SocketController — receives the shared MessageService.
 */
export const socketController = new SocketController(messageService);

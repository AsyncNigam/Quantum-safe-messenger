import { Server as SocketIOServer, Socket } from 'socket.io';
import { MessageService } from '../../services/MessageService';
export declare class SocketController {
    private readonly messageService;
    constructor(messageService: MessageService);
    private isUserConnected;
    /**
     * Binds all socket event listeners.
     *
     * NOTE: By the time this runs, `socketAuthMiddleware` has already verified
     * the Supabase JWT and attached `socket.data.user`. We trust that value
     * completely — no need to re-validate the userId here.
     */
    handleConnection: (io: SocketIOServer, socket: Socket) => void;
}
//# sourceMappingURL=SocketController.d.ts.map
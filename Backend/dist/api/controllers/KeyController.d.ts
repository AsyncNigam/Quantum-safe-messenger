import { Request, Response, NextFunction } from 'express';
import { KeyRepository } from '../../repositories/KeyRepository';
export declare class KeyController {
    private readonly keyRepo;
    constructor(keyRepo: KeyRepository);
    /**
     * POST /upload
     * Authenticated route. Uploads the user's hybrid key bundle.
     */
    upload: (req: Request, res: Response, next: NextFunction) => Promise<void>;
    /**
     * GET /
     * Returns a paginated list of all users' public keys for peer discovery.
     */
    getPaginatedKeys: (req: Request, res: Response, next: NextFunction) => Promise<void>;
}
//# sourceMappingURL=KeyController.d.ts.map
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.keyService = void 0;
const repositories_1 = require("../repositories");
exports.keyService = {
    /**
     * Fetches a paginated list of public keys.
     * Delegates storage concerns entirely to the repository layer.
     */
    async syncKeys(options) {
        return repositories_1.keyRepository.findKeysPaginated(options.page, options.limit);
    },
    /**
     * Uploads a user's hybrid key bundle.
     */
    async uploadKeys(keyData) {
        return repositories_1.keyRepository.uploadKeys(keyData);
    },
};
//# sourceMappingURL=keyService.js.map
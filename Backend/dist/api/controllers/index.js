"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __exportStar = (this && this.__exportStar) || function(m, exports) {
    for (var p in m) if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports, p)) __createBinding(exports, m, p);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.socketController = exports.keyController = void 0;
const repositories_1 = require("../../repositories");
const services_1 = require("../../services");
const KeyController_1 = require("./KeyController");
const SocketController_1 = require("./SocketController");
/**
 * Singleton KeyController — receives the shared KeyRepository.
 */
exports.keyController = new KeyController_1.KeyController(repositories_1.keyRepository);
/**
 * Singleton SocketController — receives the shared MessageService.
 */
exports.socketController = new SocketController_1.SocketController(services_1.messageService);
// Export classes for type safety in other files
__exportStar(require("./KeyController"), exports);
__exportStar(require("./SocketController"), exports);
//# sourceMappingURL=index.js.map
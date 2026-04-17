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
exports.keyService = exports.messageService = void 0;
const redis_1 = require("../config/redis");
const MessageService_1 = require("./MessageService");
const keyService_1 = require("./keyService");
Object.defineProperty(exports, "keyService", { enumerable: true, get: function () { return keyService_1.keyService; } });
/**
 * Singleton MessageService instance.
 * Receives the dedicated Redis store client via constructor injection.
 */
exports.messageService = new MessageService_1.MessageService(redis_1.storeClient);
// Export classes for type safety in other files
__exportStar(require("./MessageService"), exports);
__exportStar(require("./keyService"), exports);
//# sourceMappingURL=index.js.map
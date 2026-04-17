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
exports.keyRepository = void 0;
const supabase_1 = require("../config/supabase");
const KeyRepository_1 = require("./KeyRepository");
/**
 * Singleton KeyRepository instance.
 * The Supabase client is injected here once and shared across the app.
 */
exports.keyRepository = new KeyRepository_1.KeyRepository(supabase_1.supabase);
// Export the class for type safety in other files
__exportStar(require("./KeyRepository"), exports);
//# sourceMappingURL=index.js.map
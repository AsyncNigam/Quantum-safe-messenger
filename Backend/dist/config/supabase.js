"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.supabase = void 0;
const supabase_js_1 = require("@supabase/supabase-js");
const env_1 = require("./env");
/**
 * Singleton Supabase client.
 * Import this instance anywhere database access is needed.
 */
exports.supabase = (0, supabase_js_1.createClient)(env_1.supabaseConfig.url, env_1.supabaseConfig.anonKey);
//# sourceMappingURL=supabase.js.map
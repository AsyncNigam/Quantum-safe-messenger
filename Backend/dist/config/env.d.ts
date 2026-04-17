export interface AppConfig {
    port: number;
    clientOrigin: string;
}
export interface SupabaseConfig {
    url: string;
    anonKey: string;
}
export interface RedisConfig {
    url: string;
}
export declare const appConfig: AppConfig;
export declare const supabaseConfig: SupabaseConfig;
export declare const redisConfig: RedisConfig;
//# sourceMappingURL=env.d.ts.map
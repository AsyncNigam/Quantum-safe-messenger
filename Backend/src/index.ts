import 'dotenv/config';
import express, { Application, Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { createClient } from '@supabase/supabase-js';

// ─── Env Validation ──────────────────────────────────────────────────────────

const {
  PORT = '3000',
  SUPABASE_URL,
  SUPABASE_ANON_KEY,
} = process.env;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
  console.error('[Fatal] Missing SUPABASE_URL or SUPABASE_ANON_KEY in environment.');
  process.exit(1);
}

// ─── Supabase Client ─────────────────────────────────────────────────────────

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ─── Express App ─────────────────────────────────────────────────────────────

const app: Application = express();

app.use(cors());
app.use(express.json());

// ─── Health Check ─────────────────────────────────────────────────────────────

app.get('/health', (_req: Request, res: Response) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// ─── GET /keys/sync ───────────────────────────────────────────────────────────
/**
 * Fetches paginated public keys from the `public_keys` table in Supabase.
 *
 * Query params:
 *   - page  (number, default: 1)  — page number (1-indexed)
 *   - limit (number, default: 20) — number of records per page (max: 100)
 *
 * Response:
 *   {
 *     data:       PublicKey[],
 *     page:       number,
 *     limit:      number,
 *     total:      number,
 *     totalPages: number,
 *   }
 */
app.get('/keys/sync', async (req: Request, res: Response, next: NextFunction) => {
  try {
    // Parse & validate pagination params
    const page  = Math.max(1, parseInt((req.query.page  as string) || '1',  10));
    const limit = Math.min(100, Math.max(1, parseInt((req.query.limit as string) || '20', 10)));
    const from  = (page - 1) * limit;
    const to    = from + limit - 1;

    // Query Supabase with pagination and a count
    const { data, error, count } = await supabase
      .from('public_keys')
      .select('*', { count: 'exact' })
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('[Supabase Error] /keys/sync →', error.message);
      res.status(502).json({
        error: 'Database error',
        details: error.message,
      });
      return;
    }

    const total      = count ?? 0;
    const totalPages = Math.ceil(total / limit);

    res.json({
      data,
      page,
      limit,
      total,
      totalPages,
    });
  } catch (err) {
    next(err);
  }
});

// ─── Global Error Handler ─────────────────────────────────────────────────────

app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error('[Unhandled Error]', err.message);
  res.status(500).json({ error: 'Internal server error', details: err.message });
});

// ─── Start Server ─────────────────────────────────────────────────────────────

app.listen(Number(PORT), () => {
  console.log(`✅ Quantum Messenger API running on http://localhost:${PORT}`);
});

export default app;

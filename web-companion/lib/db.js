import { Pool } from 'pg';

let pool;

if (!global.pgPool) {
  global.pgPool = new Pool({
    connectionString: process.env.NEON_DATABASE_URL,
    max: 10,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
  });
}
pool = global.pgPool;

export async function query(text, params) {
  const start = Date.now();
  const res = await pool.query(text, params);
  const duration = Date.now() - start;
  console.log('Executed query:', { text, duration, rows: res.rowCount });
  return res;
}

export default pool;

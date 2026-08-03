import { query, getClient } from './pool';
import { logger } from '../utils/logger';

export interface Migration {
  id: number;
  name: string;
  checksum: string;
  applied_at: Date;
}

const MIGRATIONS_TABLE = 'schema_migrations';

export async function ensureMigrationsTable(): Promise<void> {
  await query(`
    CREATE TABLE IF NOT EXISTS ${MIGRATIONS_TABLE} (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      checksum VARCHAR(64) NOT NULL,
      applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);
}

export async function getAppliedMigrations(): Promise<Migration[]> {
  const result = await query<Migration>(
    `SELECT id, name, checksum, applied_at FROM ${MIGRATIONS_TABLE} ORDER BY id`
  );
  return result.rows;
}

export async function recordMigration(name: string, checksum: string): Promise<void> {
  await query(
    `INSERT INTO ${MIGRATIONS_TABLE} (name, checksum) VALUES ($1, $2)`,
    [name, checksum]
  );
}

export async function runMigrations(migrations: Array<{ name: string; up: string; checksum: string }>): Promise<void> {
  await ensureMigrationsTable();
  
  const applied = await getAppliedMigrations();
  const appliedNames = new Set(applied.map(m => m.name));
  
  for (const migration of migrations) {
    if (appliedNames.has(migration.name)) {
      const appliedMigration = applied.find(m => m.name === migration.name);
      if (appliedMigration && appliedMigration.checksum !== migration.checksum) {
        throw new Error(`Migration ${migration.name} has been modified after application`);
      }
      continue;
    }
    
    console.log(`Applying migration: ${migration.name}`);
    await query(migration.up);
    await recordMigration(migration.name, migration.checksum);
    console.log(`Applied migration: ${migration.name}`);
  }
  
  console.log('All migrations applied successfully');
}

export async function rollbackMigration(name: string, downSql: string): Promise<void> {
  await query(downSql);
  await query(`DELETE FROM ${MIGRATIONS_TABLE} WHERE name = $1`, [name]);
}
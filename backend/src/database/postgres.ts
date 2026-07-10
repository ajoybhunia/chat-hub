// ...existing code...
import { Client } from "@db/postgres";
import { config } from "../config/env.ts";

export const db = new Client(config.databaseUrl);
await db.connect();

export async function migrateDatabase() {
  const migrationPath = new URL(
    "./migrations/001_create_users.sql",
    import.meta.url,
  );
  const migrationSql = await Deno.readTextFile(migrationPath);
  const statements = migrationSql
    .split(";")
    .map((sql) => sql.trim())
    .filter(Boolean);

  for (const statement of statements) {
    await db.queryObject(statement);
  }
}

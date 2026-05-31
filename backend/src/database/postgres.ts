// ...existing code...
import { Client } from "jsr:@db/postgres";
import { config } from "../config/env.ts";

export const db = new Client(config.databaseUrl);
await db.connect();

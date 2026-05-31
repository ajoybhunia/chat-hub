// ...existing code...
import { db } from "../../database/postgres.ts";

interface DbUser {
  id: number;
  username: string;
  email: string;
  password_hash: string;
}

export class AuthRepository {
  async findByEmail(email: string): Promise<DbUser | null> {
    const result = await db.queryObject<DbUser>(
      "SELECT * FROM users WHERE email = $1",
      [email],
    );
    return result.rows[0] ?? null;
  }

  async findByUsername(username: string): Promise<DbUser | null> {
    const result = await db.queryObject<DbUser>(
      "SELECT * FROM users WHERE username = $1",
      [username],
    );
    return result.rows[0] ?? null;
  }

  async createUser(
    username: string,
    email: string,
    passwordHash: string,
  ): Promise<DbUser> {
    const result = await db.queryObject<DbUser>(
      "INSERT INTO users (username, email, password_hash) VALUES ($1, $2, $3) RETURNING *",
      [username, email, passwordHash],
    );
    return result.rows[0];
  }
}

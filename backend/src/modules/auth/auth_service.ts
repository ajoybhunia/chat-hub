// ...existing code...
import { AuthRepository } from "./auth_repository.ts";
import { createJwt } from "../../utils/jwt.ts";
import bcrypt from "npm:bcryptjs";

export class AuthService {
  private repo = new AuthRepository();

  async signup(username: string, email: string, password: string) {
    const existing = await this.repo.findByEmail(email);
    if (existing) throw new Error("Email already in use");
    const existingUser = await this.repo.findByUsername(username);
    if (existingUser) throw new Error("Username already in use");
    const passwordHash = await bcrypt.hash(password, 10);
    const user = await this.repo.createUser(username, email, passwordHash);
    const token = await createJwt({ id: user.id, email: user.email });
    return { user, token };
  }

  async login(email: string, password: string) {
    const user = await this.repo.findByEmail(email);
    if (!user) throw new Error("Invalid credentials");
    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) throw new Error("Invalid credentials");
    const token = await createJwt({ id: user.id, email: user.email });
    return { user, token };
  }
}

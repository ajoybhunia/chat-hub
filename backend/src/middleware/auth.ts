// ...existing code...
import { verifyJwt } from "../utils/jwt.ts";

export async function authMiddleware(ctx: any, next: () => Promise<void>) {
  const authHeader = ctx.request.headers.get("Authorization");
  if (!authHeader) {
    ctx.response.status = 401;
    ctx.response.body = { error: "Missing Authorization header" };
    return;
  }
  const token = authHeader.replace("Bearer ", "");
  try {
    ctx.state.user = await verifyJwt(token);
    await next();
  } catch {
    ctx.response.status = 401;
    ctx.response.body = { error: "Invalid or expired token" };
  }
}

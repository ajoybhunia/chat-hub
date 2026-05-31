import type { JWTPayload } from "npm:jose";
import { verifyJwt } from "../utils/jwt.ts";

const json401 = (message: string) =>
  new Response(JSON.stringify({ error: message }), {
    status: 401,
    headers: { "Content-Type": "application/json" },
  });

export async function requireAuth(
  request: Request,
): Promise<JWTPayload | Response> {
  const authHeader = request.headers.get("Authorization");
  if (!authHeader) {
    return json401("Missing Authorization header");
  }
  const token = authHeader.replace("Bearer ", "");
  try {
    return await verifyJwt(token);
  } catch {
    return json401("Invalid or expired token");
  }
}

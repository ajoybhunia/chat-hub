// ...existing code...
import { AuthService } from "./auth_service.ts";

const authService = new AuthService();

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

export async function handleAuthRoutes(request: Request): Promise<Response> {
  const url = new URL(request.url);

  if (url.pathname.endsWith("/signup") && request.method === "POST") {
    try {
      const { username, email, password } = await request.json();
      const { user, token } = await authService.signup(username, email, password);
      return json({ user, token }, 201);
    } catch (e: any) {
      return json({ error: e.message }, 400);
    }
  }

  if (url.pathname.endsWith("/login") && request.method === "POST") {
    try {
      const { email, password } = await request.json();
      const { user, token } = await authService.login(email, password);
      return json({ user, token });
    } catch (e: any) {
      return json({ error: e.message }, 400);
    }
  }

  return json({ error: "Not found" }, 404);
}

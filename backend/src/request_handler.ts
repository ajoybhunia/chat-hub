import {
  handleOnclose,
  handleOnmessage,
  handleOnopen,
} from "./socket_handler.ts";
import { handleAuthRoutes } from "./modules/auth/auth_handler.ts";
import { requireAuth } from "./middleware/auth.ts";
import { verifyJwt } from "./utils/jwt.ts";

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

function withCors(response: Response): Response {
  const headers = new Headers(response.headers);
  for (const [k, v] of Object.entries(CORS_HEADERS)) headers.set(k, v);
  return new Response(response.body, { status: response.status, headers });
}

const handleRequest = async (
  request: Request,
  clients: Set<WebSocket>,
): Promise<Response> => {
  if (request.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  const url = new URL(request.url);

  if (url.pathname.startsWith("/auth/")) {
    return withCors(await handleAuthRoutes(request));
  }

  if (request.headers.get("upgrade") === "websocket") {
    // Browser WebSocket API cannot set headers; token must be in ?token= query param
    const wsToken = url.searchParams.get("token");
    if (!wsToken) {
      return withCors(
        new Response(JSON.stringify({ error: "Missing Authorization header" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      );
    }
    try {
      await verifyJwt(wsToken);
    } catch {
      return withCors(
        new Response(JSON.stringify({ error: "Invalid or expired token" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      );
    }
    const { socket, response } = Deno.upgradeWebSocket(request);
    clients.add(socket);
    socket.onopen = (ev: Event) => handleOnopen(ev, clients);
    socket.onmessage = (ev: MessageEvent) => handleOnmessage(ev, clients);
    socket.onclose = (ev: CloseEvent) => handleOnclose(ev, clients, socket);
    return response;
  }

  const authResult = await requireAuth(request);
  if (authResult instanceof Response) {
    return withCors(authResult);
  }

  // authResult is the verified JWTPayload — available as second arg when future handlers are added
  return new Response("Not found", { status: 404, headers: CORS_HEADERS });
};

export default handleRequest;

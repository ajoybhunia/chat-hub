/**
 * BFF (backend-for-frontend) — a Vite plugin, no separate process or port.
 *
 * The browser only ever talks to its own origin:
 *   HTTP  /api/*          -> stripped, forwarded to UPSTREAM_URL (Spring backend)
 *   WS    /socket?token=  -> upgraded, piped to UPSTREAM_URL "/" (root endpoint)
 *
 * Runs in both `vite dev` (configureServer) and `vite preview`
 * (configurePreviewServer). Vite's own HMR websocket is unaffected: it only
 * claims upgrades carrying the `vite-hmr` subprotocol on its HMR base path,
 * which browser WebSockets never send.
 */
import { request as httpRequest } from "node:http";
import { request as httpsRequest } from "node:https";
import type { IncomingMessage, ServerResponse } from "node:http";
import type { Socket } from "node:net";
import type { Connect, Plugin, PreviewServer } from "vite";
import { API_PREFIX, SOCKET_PATH } from "../src/constants/paths";
import { ACTION_TYPE, log } from "../src/lib/logger";

export interface BffOptions {
  /** Origin of the backend, e.g. "http://localhost:8000" (no path). */
  upstream: string;
}

function makeRequest(
  upstream: URL,
  options: { method?: string; path: string; headers: IncomingMessage["headers"] },
  callback: (res: IncomingMessage) => void,
): ReturnType<typeof httpRequest> {
  const common = {
    hostname: upstream.hostname,
    port: upstream.port || undefined,
    method: options.method,
    path: options.path,
    headers: options.headers,
  };
  return upstream.protocol === "https:"
    ? httpsRequest({ ...common, protocol: upstream.protocol }, callback)
    : httpRequest({ ...common, protocol: upstream.protocol }, callback);
}

/** HTTP passthrough: /api/* -> upstream, everything else falls through to Vite. */
function handleHttp(
  upstream: URL,
  req: IncomingMessage,
  res: ServerResponse,
  next: Connect.NextFunction,
): void {
  const requestUrl = req.url ?? "/";
  const isApi =
    requestUrl === API_PREFIX ||
    requestUrl.startsWith(`${API_PREFIX}/`) ||
    requestUrl.startsWith(`${API_PREFIX}?`);
  if (!isApi) {
    next();
    return;
  }

  const stripped = requestUrl.slice(API_PREFIX.length) || "/";
  const target = stripped.startsWith("/") ? stripped : `/${stripped}`;
  const started = Date.now();
  // Log the pathname only — queries never reach the log line.
  const path = target.split("?")[0] ?? "/";
  const method = req.method ?? "GET";

  res.on("finish", () => {
    log.info(
      {
        action_type: ACTION_TYPE.PROXY_REQUEST,
        method,
        path,
        status: res.statusCode,
        duration_ms: Date.now() - started,
      },
      "Proxy request",
    );
  });

  const proxyReq = makeRequest(
    upstream,
    { method, path: target, headers: { ...req.headers, host: upstream.host } },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode ?? 502, proxyRes.statusMessage, proxyRes.headers);
      proxyRes.pipe(res);
    },
  );

  proxyReq.on("error", (err) => {
    log.error(
      { action_type: ACTION_TYPE.PROXY_ERROR, method, path, err },
      "Upstream request failed",
    );
    if (!res.headersSent) {
      res.writeHead(502, { "content-type": "application/json" });
      res.end(JSON.stringify({ error: "Bad gateway: backend unreachable" }));
    } else {
      res.end();
    }
  });

  req.pipe(proxyReq);
}

/** WebSocket passthrough: /socket -> upstream "/" (token query preserved). */
function handleUpgrade(
  upstream: URL,
  req: IncomingMessage,
  socket: Socket,
  head: Buffer,
): void {
  const pathname = (req.url ?? "/").split("?")[0];
  if (pathname !== SOCKET_PATH) {
    return; // not ours — leave it to Vite (HMR claims only vite-hmr subprotocol upgrades)
  }

  const started = Date.now();
  const rawUrl = req.url ?? "/";
  const queryIndex = rawUrl.indexOf("?");
  const target = queryIndex === -1 ? "/" : `/${rawUrl.slice(queryIndex)}`;

  const rejectWith = (status: number, message: string) => {
    log.warn(
      { action_type: ACTION_TYPE.WS_UPGRADE, status, path: SOCKET_PATH },
      message,
    );
    socket.write(`HTTP/1.1 ${status} ${message}\r\nConnection: close\r\n\r\n`);
    socket.destroy();
  };

  const proxyReq = makeRequest(
    upstream,
    {
      method: "GET",
      path: target,
      headers: { ...req.headers, host: upstream.host },
    },
    () => {
      // A regular response means the backend refused to upgrade (e.g. 403).
      rejectWith(400, "Upstream did not switch protocols");
    },
  );

  proxyReq.on("upgrade", (proxyRes, proxySocket, proxyHead) => {
    const lines = [
      `HTTP/1.1 ${proxyRes.statusCode ?? 101} ${proxyRes.statusMessage ?? "Switching Protocols"}`,
    ];
    for (let i = 0; i < proxyRes.rawHeaders.length; i += 2) {
      lines.push(`${proxyRes.rawHeaders[i]}: ${proxyRes.rawHeaders[i + 1]}`);
    }
    socket.write(`${lines.join("\r\n")}\r\n\r\n`);

    if (proxyHead?.length) socket.write(proxyHead);
    if (head?.length) proxySocket.write(head);

    const teardown = () => {
      socket.destroy();
      proxySocket.destroy();
    };
    socket.on("error", teardown);
    proxySocket.on("error", teardown);

    proxySocket.pipe(socket);
    socket.pipe(proxySocket);

    log.info(
      {
        action_type: ACTION_TYPE.WS_UPGRADE,
        path: SOCKET_PATH,
        status: proxyRes.statusCode ?? 101,
        duration_ms: Date.now() - started,
      },
      "WebSocket upgraded via BFF",
    );
  });

  proxyReq.on("response", (proxyRes) => {
    const status = proxyRes.statusCode ?? 502;
    proxyRes.resume(); // discard body
    rejectWith(status, "Upstream rejected WebSocket upgrade");
  });

  proxyReq.on("error", (err) => {
    log.error(
      { action_type: ACTION_TYPE.PROXY_ERROR, path: SOCKET_PATH, err },
      "WebSocket upstream connection failed",
    );
    socket.destroy();
  });

  proxyReq.end();
}

interface UpgradeEmitter {
  on(
    event: "upgrade",
    listener: (req: IncomingMessage, socket: Socket, head: Buffer) => void,
  ): unknown;
}

function attach(
  upstream: URL,
  middlewares: Connect.Server,
  httpServer: UpgradeEmitter | null | undefined,
): void {
  middlewares.use((req, res, next) => handleHttp(upstream, req, res, next));
  httpServer?.on("upgrade", (req, socket, head) =>
    handleUpgrade(upstream, req, socket, head),
  );
}

export function bffPlugin(options: BffOptions): Plugin {
  const upstream = new URL(options.upstream);

  return {
    name: "chat-hub-bff",

    configureServer(server) {
      attach(upstream, server.middlewares, server.httpServer);
    },

    configurePreviewServer(server: PreviewServer) {
      attach(upstream, server.middlewares, server.httpServer);
    },
  };
}

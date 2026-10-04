/**
 * Structured logger — context object first, message second.
 *
 *   log.info({ status: 200, action_type: ACTION_TYPE.LOGIN }, 'Login succeeded');
 *
 * - Context keys are snake_case; errors go in `err` (serialized to {name, message},
 *   plus stack in pretty/dev format).
 * - Output: pretty one-liners in dev, single-line JSON (backend logback style) in
 *   production bundles. The BFF (Node context, no import.meta.env) sets the format
 *   explicitly via configureLogger() from vite.config.ts.
 * - Sensitive keys (`token`, `password`, `authorization`) are redacted, and any
 *   string containing `?token=…` is scrubbed, so secrets never reach the console.
 */

export const ACTION_TYPE = {
  LOGIN: "login",
  SIGNUP: "signup",
  LOGOUT: "logout",
  WS_CONNECT: "ws_connect",
  WS_DISCONNECT: "ws_disconnect",
  WS_MESSAGE_SENT: "ws_message_sent",
  WS_MESSAGE_RECEIVED: "ws_message_received",
  PROXY_REQUEST: "proxy_request",
  PROXY_ERROR: "proxy_error",
  WS_UPGRADE: "ws_upgrade",
} as const;

export type ActionType = (typeof ACTION_TYPE)[keyof typeof ACTION_TYPE];

export type LogFormat = "pretty" | "json";
export type LogLevel = "debug" | "info" | "warn" | "error";

type LogContext = Record<string, unknown>;

const LEVEL_ORDER: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

const REDACTED_KEYS = new Set(["token", "password", "authorization"]);
const TOKEN_IN_STRING = /([?&]token=)[^&\s]+/gi;

let forcedFormat: LogFormat | undefined;
let forcedMinLevel: LogLevel | undefined;

/** Explicit override for contexts without import.meta.env (the BFF/Node side). */
export function configureLogger(options: { format?: LogFormat; minLevel?: LogLevel }): void {
  if (options.format) forcedFormat = options.format;
  if (options.minLevel) forcedMinLevel = options.minLevel;
}

/** true in a Vite dev bundle, false in a production bundle, undefined in Node. */
function devFlag(): boolean | undefined {
  try {
    const meta = import.meta as unknown as { env?: { DEV?: boolean } };
    return meta.env?.DEV;
  } catch {
    return undefined;
  }
}

function resolveFormat(): LogFormat {
  if (forcedFormat) return forcedFormat;
  const flag = devFlag();
  if (flag === undefined) return "pretty";
  return flag ? "pretty" : "json";
}

function resolveMinLevel(): LogLevel {
  if (forcedMinLevel) return forcedMinLevel;
  const flag = devFlag();
  if (flag === undefined) return "debug";
  return flag ? "debug" : "info";
}

function serializeError(value: unknown): unknown {
  if (value instanceof Error) {
    const base: Record<string, unknown> = { name: value.name, message: value.message };
    if (value.stack && resolveFormat() === "pretty") base.stack = value.stack;
    return base;
  }
  return value;
}

function sanitize(context: LogContext): LogContext {
  const out: LogContext = {};
  for (const [key, value] of Object.entries(context)) {
    if (REDACTED_KEYS.has(key.toLowerCase())) {
      out[key] = "***";
    } else if (key === "err") {
      out[key] = serializeError(value);
    } else if (typeof value === "string") {
      out[key] = value.replace(TOKEN_IN_STRING, "$1***");
    } else {
      out[key] = value;
    }
  }
  return out;
}

function emit(level: LogLevel, context: LogContext, message: string): void {
  if (LEVEL_ORDER[level] < LEVEL_ORDER[resolveMinLevel()]) return;

  const ctx = sanitize(context);
  const method = level as "debug" | "info" | "warn" | "error";

  if (resolveFormat() === "json") {
    console[method](
      JSON.stringify({ level, time: new Date().toISOString(), msg: message, ...ctx }),
    );
  } else if (Object.keys(ctx).length > 0) {
    console[method](`[${level}] ${message}`, ctx);
  } else {
    console[method](`[${level}] ${message}`);
  }
}

export const log = {
  debug: (context: LogContext, message: string): void => emit("debug", context, message),
  info: (context: LogContext, message: string): void => emit("info", context, message),
  warn: (context: LogContext, message: string): void => emit("warn", context, message),
  error: (context: LogContext, message: string): void => emit("error", context, message),
};

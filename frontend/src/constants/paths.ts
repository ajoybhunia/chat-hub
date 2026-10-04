/**
 * Shared route constants — no DOM or Node APIs, so they can be imported
 * both from client code (src/) and the BFF plugin (bff/).
 */

/** All client HTTP calls go through this prefix; the BFF strips it before forwarding. */
export const API_PREFIX = "/api";

/** Browser WebSocket path; the BFF rewrites it to the upstream root ("/"). */
export const SOCKET_PATH = "/socket";

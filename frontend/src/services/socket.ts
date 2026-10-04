import { SOCKET_PATH } from "../constants/paths";

/**
 * Same-origin WebSocket URL — the BFF proxies SOCKET_PATH to the backend's
 * root endpoint, so the browser never addresses the backend directly.
 */
export function buildSocketUrl(token: string): string {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}${SOCKET_PATH}?token=${encodeURIComponent(token)}`;
}

// ...existing code...
import { useEffect, useRef } from "react";
import { useChatStore } from "../store/chat.store";
import { useAuthStore } from "../store/auth.store";
import { buildSocketUrl } from "../services/socket";
import { ACTION_TYPE, log } from "../lib/logger";
import { SOCKET_PATH } from "../constants/paths";
import type { Message } from "../types/chat";

export function useSocket() {
  const socketRef = useRef<WebSocket | null>(null);
  const addMessage = useChatStore((state) => state.addMessage);
  const user = useAuthStore((state) => state.user);
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    if (!token) {
      if (socketRef.current) {
        log.info(
          { action_type: ACTION_TYPE.WS_DISCONNECT, path: SOCKET_PATH },
          "WebSocket closing because auth token is missing",
        );
        socketRef.current.close();
      }
      socketRef.current = null;
      return;
    }

    log.debug(
      { action_type: ACTION_TYPE.WS_CONNECT, path: SOCKET_PATH },
      "Connecting WebSocket",
    );
    const ws = new WebSocket(buildSocketUrl(token));
    socketRef.current = ws;

    ws.onopen = () => {
      log.info(
        { action_type: ACTION_TYPE.WS_CONNECT, path: SOCKET_PATH },
        "WebSocket connected",
      );
    };

    ws.onmessage = (ev) => {
      try {
        const message: Message = JSON.parse(ev.data);
        addMessage(message);
        log.debug(
          { action_type: ACTION_TYPE.WS_MESSAGE_RECEIVED, user: message.user },
          "Message received",
        );
      } catch {
        // ignore malformed frames
        log.debug(
          { action_type: ACTION_TYPE.WS_MESSAGE_RECEIVED, detail: "malformed frame" },
          "Ignored malformed WebSocket frame",
        );
      }
    };

    ws.onerror = () => {
      log.error(
        { action_type: ACTION_TYPE.WS_CONNECT, path: SOCKET_PATH, detail: "client error event" },
        "WebSocket error",
      );
    };

    ws.onclose = (event) => {
      log.warn(
        {
          action_type: ACTION_TYPE.WS_DISCONNECT,
          code: event.code,
          reason: event.reason,
          was_clean: event.wasClean,
        },
        "WebSocket closed",
      );
      if (socketRef.current === ws) {
        socketRef.current = null;
      }
    };

    return () => {
      if (
        ws.readyState === WebSocket.OPEN ||
        ws.readyState === WebSocket.CONNECTING
      ) {
        ws.close();
      }
      if (socketRef.current === ws) {
        socketRef.current = null;
      }
    };
  }, [addMessage, token]);

  const sendMessage = (content: string) => {
    if (socketRef.current?.readyState === WebSocket.OPEN && user) {
      socketRef.current.send(JSON.stringify({ user: user.username, content }));
      log.debug({ action_type: ACTION_TYPE.WS_MESSAGE_SENT }, "Message sent");
    }
  };

  return { sendMessage };
}

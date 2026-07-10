// ...existing code...
import { useEffect, useRef } from "react";
import { useChatStore } from "../store/chat.store";
import { useAuthStore } from "../store/auth.store";
import type { Message } from "../types/chat";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8000";
const WS_BASE_URL = API_URL.replace(/^http/, "ws");

export function useSocket() {
  const socketRef = useRef<WebSocket | null>(null);
  const addMessage = useChatStore((state) => state.addMessage);
  const user = useAuthStore((state) => state.user);
  const token = useAuthStore((state) => state.token);

  useEffect(() => {
    if (!token) {
      if (socketRef.current) {
        console.log("WebSocket closing because auth token is missing");
        socketRef.current.close();
      }
      socketRef.current = null;
      return;
    }

    const url = `${WS_BASE_URL}/?token=${encodeURIComponent(token)}`;
    console.log("Connecting WebSocket to", url);
    const ws = new WebSocket(url);
    socketRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
    };

    ws.onmessage = (ev) => {
      try {
        const message: Message = JSON.parse(ev.data);
        addMessage(message);
      } catch {
        // ignore malformed frames
      }
    };

    ws.onerror = (event) => {
      console.error("WebSocket error:", event);
    };

    ws.onclose = (event) => {
      console.warn(
        "WebSocket closed:",
        event.code,
        event.reason,
        event.wasClean,
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
    }
  };

  return { sendMessage };
}

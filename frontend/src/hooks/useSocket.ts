// ...existing code...
import { useEffect, useRef } from "react";
import { useChatStore } from "../store/chat.store";
import { useAuthStore } from "../store/auth.store";
import type { Message } from "../types/chat";

const WS_URL = (import.meta.env.VITE_API_URL || "http://localhost:8000").replace(
  /^http/,
  "ws",
);

export function useSocket() {
  const socketRef = useRef<WebSocket | null>(null);
  const addMessage = useChatStore((state) => state.addMessage);
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    const ws = new WebSocket(WS_URL);
    socketRef.current = ws;

    ws.onmessage = (ev) => {
      try {
        const message: Message = JSON.parse(ev.data);
        addMessage(message);
      } catch {
        // ignore malformed frames
      }
    };

    return () => ws.close();
  }, [addMessage]);

  const sendMessage = (content: string) => {
    if (socketRef.current?.readyState === WebSocket.OPEN && user) {
      socketRef.current.send(JSON.stringify({ user: user.username, content }));
    }
  };

  return { sendMessage };
}

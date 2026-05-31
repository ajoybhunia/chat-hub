// ...existing code...
import { useState, useEffect, useRef } from "react";
import { Box, Button, Divider, Paper, TextField, Typography } from "@mui/material";
import { useChatStore } from "../../store/chat.store";
import { useSocket } from "../../hooks/useSocket";

export default function ChatPanel() {
  const [input, setInput] = useState("");
  const messages = useChatStore((state) => state.messages);
  const { sendMessage } = useSocket();
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    sendMessage(trimmed);
    setInput("");
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Box sx={{ display: "flex", flexDirection: "column", flex: 1, height: "100vh", overflow: "hidden" }}>
      <Box sx={{ px: 3, py: 2, borderBottom: 1, borderColor: "divider" }}>
        <Typography variant="h6" sx={{ fontWeight: "medium" }}># general</Typography>
      </Box>

      <Box
        sx={{
          flex: 1,
          overflow: "auto",
          p: 2,
          display: "flex",
          flexDirection: "column",
          gap: 1,
        }}
      >
        {messages.length === 0 ? (
          <Typography color="text.secondary" sx={{ m: "auto", textAlign: "center" }}>
            No messages yet — say something!
          </Typography>
        ) : (
          messages.map((msg, i) => (
            <Paper key={i} variant="outlined" sx={{ p: 1.5, maxWidth: "70%", alignSelf: "flex-start" }}>
              <Box sx={{ display: "flex", alignItems: "baseline", gap: 1, mb: 0.5 }}>
                <Typography variant="caption" sx={{ fontWeight: "bold", color: "primary.main" }}>
                  {msg.user}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {new Date(msg.timestamp).toLocaleTimeString()}
                </Typography>
              </Box>
              <Typography variant="body2">{msg.content}</Typography>
            </Paper>
          ))
        )}
        <div ref={bottomRef} />
      </Box>

      <Divider />
      <Box sx={{ p: 2, display: "flex", gap: 1, alignItems: "flex-end" }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Message #general"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          multiline
          maxRows={4}
        />
        <Button
          variant="contained"
          onClick={handleSend}
          disabled={!input.trim()}
          sx={{ minWidth: 80 }}
        >
          Send
        </Button>
      </Box>
    </Box>
  );
}

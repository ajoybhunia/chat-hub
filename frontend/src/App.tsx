import { useEffect, useRef, useState } from "react";

type ChatMessage = {
  user: string;
  content: string;
  timestamp: string;
};

const WS_URL = "ws://localhost:8000";

const App = () => {
  const [username, setUsername] = useState(() =>
    `User${Math.floor(Math.random() * 1000)}`
  );
  const [inputValue, setInputValue] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState("Connecting...");
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const socket = new WebSocket(WS_URL);
    wsRef.current = socket;

    socket.addEventListener("open", () => {
      setStatus("Connected");
    });

    socket.addEventListener("message", (event) => {
      try {
        const data = JSON.parse(event.data) as ChatMessage;
        setMessages((prev) => [...prev, data]);
      } catch (error) {
        console.error("Invalid message from server:", error);
      }
    });

    socket.addEventListener("close", () => {
      setStatus("Disconnected");
    });

    socket.addEventListener("error", () => {
      setStatus("Error");
    });

    return () => {
      socket.close();
    };
  }, []);

  const sendMessage = (event: React.SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    const content = inputValue.trim();
    if (
      !content || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN
    ) {
      return;
    }

    wsRef.current.send(JSON.stringify({ user: username, content }));
    setInputValue("");
  };

  return (
    <main
      style={{
        fontFamily: "sans-serif",
        maxWidth: 760,
        margin: "0 auto",
        padding: 20,
      }}
    >
      <header style={{ marginBottom: 20 }}>
        <h1>Chat Hub</h1>
        <p style={{ margin: "4px 0" }}>
          Status: <strong>{status}</strong>
        </p>
        <label style={{ display: "block", marginBottom: 10 }}>
          Your name:
          <input
            style={{ marginLeft: 8, padding: 6, minWidth: 180 }}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
          />
        </label>
      </header>

      <section
        style={{
          border: "1px solid #ddd",
          borderRadius: 8,
          padding: 16,
          minHeight: 320,
          marginBottom: 16,
          background: "#fafafa",
        }}
      >
        {messages.length === 0
          ? (
            <p style={{ color: "#666" }}>
              No messages yet. Send the first message.
            </p>
          )
          : (
            <div style={{ display: "grid", gap: 12 }}>
              {messages.map((message, index) => (
                <div
                  key={`${message.timestamp}-${index}`}
                  style={{
                    padding: 12,
                    borderRadius: 8,
                    background: "white",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
                  }}
                >
                  <div style={{ fontSize: 12, color: "#888", marginBottom: 4 }}>
                    {message.user} •{" "}
                    {new Date(message.timestamp).toLocaleTimeString()}
                  </div>
                  <div>{message.content}</div>
                </div>
              ))}
            </div>
          )}
      </section>

      <form onSubmit={sendMessage} style={{ display: "flex", gap: 8 }}>
        <input
          style={{
            flex: 1,
            padding: 10,
            borderRadius: 8,
            border: "1px solid #ccc",
          }}
          value={inputValue}
          onChange={(event) => setInputValue(event.target.value)}
          placeholder="Type a message"
          disabled={status !== "Connected"}
        />
        <button
          type="submit"
          style={{
            padding: "10px 16px",
            borderRadius: 8,
            border: "none",
            background: "#0b7cff",
            color: "white",
          }}
        >
          Send
        </button>
      </form>
    </main>
  );
};

export default App;

// import { useEffect, useState } from "react";

// type Message = {
//   user: string;
//   content: string;
//   timestamp: string;
// };

// const ws = new WebSocket("ws://localhost:8000");

// const App = () => {
//   const [messages, setMessages] = useState<Message[]>([]);
//   const [input, setInput] = useState("");

//   useEffect(() => {
//     ws.onmessage = (event) => {
//       const data = JSON.parse(event.data) as Message;

//       setMessages((prev) => [...prev, data]);
//     };
//   }, []);

//   const sendMessage = () => {
//     if (!input.trim()) return;

//     ws.send(
//       JSON.stringify({
//         user: "Rahul",
//         content: input,
//       }),
//     );

//     setInput("");
//   };

//   return (
//     <div style={{ padding: 20 }}>
//       <h1>Chat App</h1>

//       <div>
//         {messages.map((msg, index) => (
//           <div key={index}>
//             <strong>{msg.user}</strong>: {msg.content}
//           </div>
//         ))}
//       </div>

//       <input
//         value={input}
//         onChange={(e) => setInput(e.target.value)}
//       />

//       <button onClick={sendMessage}>
//         Send
//       </button>
//     </div>
//   );
// };

// export default App;

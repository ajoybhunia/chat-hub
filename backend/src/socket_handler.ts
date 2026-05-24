const handleOnopen = (_event: Event, clients: Set<WebSocket>): void => {
  console.log("Client connected. Total clients: ", clients.size);
};

const handleOnmessage = (
  event: MessageEvent,
  clients: Set<WebSocket>,
): void => {
  try {
    const message = JSON.parse(event.data);
    const formattedMessage = {
      user: message.user || "Anonymous",
      content: message.content,
      timestamp: new Date().toISOString(),
    };

    console.log("Broadcast: ", formattedMessage);

    clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(JSON.stringify(formattedMessage));
      }
    });
  } catch (e) {
    console.error("Parsing error: ", e);
  }
};

const handleOnclose = (
  _event: CloseEvent,
  clients: Set<WebSocket>,
  socket: WebSocket,
): void => {
  clients.delete(socket);
  console.log("Client disconnected. Remaining clients: ", clients.size);
};

export { handleOnclose, handleOnmessage, handleOnopen };

export const requestHandler = (req, clients) => {
  if (req.headers.get("upgrade") !== "websocket") {
    return new Response("This is not a WebSocket request");
  }

  const { socket, response } = Deno.upgradeWebSocket(req);

  clients.add(socket);

  socket.onopen = () => console.log("connected");

  socket.onmessage = (e) => {
    console.log("Received: ", e.data);

    for (const client of clients) {
      client.send("text: " + e.data);
    }
  };

  socket.onclose = () => {
    clients.delete(socket);
    console.log("Client Disconnected");
  };

  return response;
};

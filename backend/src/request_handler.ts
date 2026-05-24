import {
  handleOnclose,
  handleOnmessage,
  handleOnopen,
} from "./socket_handler.ts";

const handleRequest = (
  request: Request,
  clients: Set<WebSocket>,
): Response => {
  if (request.headers.get("upgrade") !== "websocket") {
    return new Response("This is not a websocket request");
  }

  const { socket, response } = Deno.upgradeWebSocket(request);

  clients.add(socket);

  socket.onopen = (ev: Event) => handleOnopen(ev, clients);
  socket.onmessage = (ev: MessageEvent) => handleOnmessage(ev, clients);
  socket.onclose = (ev: CloseEvent) => handleOnclose(ev, clients, socket);

  return response;
};

export default handleRequest;

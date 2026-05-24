import handleRequest from "./src/request_handler.ts";

const main = () => {
  const port = 8000;
  const clients = new Set<WebSocket>();

  Deno.serve({ port }, (req) => handleRequest(req, clients));
};

main();

import { requestHandler } from "./src/server.js";

const main = () => {
  const PORT = 8000;
  const clients = new Set();

  Deno.serve({ port: PORT }, (req) => requestHandler(req, clients));
};

main();

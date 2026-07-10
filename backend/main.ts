import handleRequest from "./src/request_handler.ts";
import { migrateDatabase } from "./src/database/postgres.ts";

const main = async () => {
  const port = 8000;
  const clients = new Set<WebSocket>();

  await migrateDatabase();

  Deno.serve({ port }, (req) => handleRequest(req, clients));
};

main();

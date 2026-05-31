// ...existing code...
function require(key: string): string {
  const value = Deno.env.get(key);
  if (!value) throw new Error(`Missing required env var: ${key}. Copy .env.example to .env and fill in values.`);
  return value;
}

export const config = {
  databaseUrl: require("DATABASE_URL"),
  redisUrl: Deno.env.get("REDIS_URL") ?? "redis://localhost:6379",
  jwtSecret: require("JWT_SECRET"),
};

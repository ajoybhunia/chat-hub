// ...existing code...
import { connect, type Redis } from "jsr:@db/redis";
import { config } from "../config/env.ts";

let _redis: Redis | null = null;

export async function getRedis(): Promise<Redis> {
  if (!_redis) {
    const [hostname, portStr] = config.redisUrl.replace("redis://", "").split(":");
    _redis = await connect({ hostname, port: Number(portStr) || 6379 });
  }
  return _redis;
}

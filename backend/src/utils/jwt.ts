// ...existing code...
import { jwtVerify, SignJWT } from "npm:jose";
import { config } from "../config/env.ts";

const secret = () => new TextEncoder().encode(config.jwtSecret);

export const createJwt = async (payload: Record<string, unknown>) =>
  new SignJWT(payload)
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime("7d")
    .sign(secret());

export const verifyJwt = async (token: string) => {
  const { payload } = await jwtVerify(token, secret());
  return payload;
};

import api from "./api";
import type { AuthResponse } from "../types/auth";

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>("/auth/login", { email, password });
  return res.data;
}

export async function signup(
  username: string,
  email: string,
  password: string,
): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>("/auth/signup", { username, email, password });
  return res.data;
}

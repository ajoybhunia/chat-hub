import { create } from "zustand";
import { persist } from "zustand/middleware";
import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8000";

interface User {
  id: number;
  username: string;
  email: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  signup: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      login: async (email, password) => {
        const res = await axios.post(`${API_URL}/auth/login`, { email, password });
        set({ user: res.data.user, token: res.data.token });
      },
      signup: async (username, email, password) => {
        const res = await axios.post(`${API_URL}/auth/signup`, {
          username,
          email,
          password,
        });
        set({ user: res.data.user, token: res.data.token });
      },
      logout: () => set({ user: null, token: null }),
    }),
    {
      name: "auth-storage",
      partialize: (state) => ({ user: state.user, token: state.token }),
    },
  ),
);

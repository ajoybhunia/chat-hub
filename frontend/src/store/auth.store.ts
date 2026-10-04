import { create } from "zustand";
import { persist } from "zustand/middleware";
import { login as loginRequest, signup as signupRequest } from "../services/auth";
import { httpStatusOf } from "../services/api";
import { ACTION_TYPE, log } from "../lib/logger";
import type { User } from "../types/auth";

interface AuthState {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  signup: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      login: async (email, password) => {
        try {
          const data = await loginRequest(email, password);
          set({ user: data.user, token: data.token });
          log.info(
            { action_type: ACTION_TYPE.LOGIN, status: 200, user_id: data.user.id },
            "Login succeeded",
          );
        } catch (err) {
          log.warn(
            { action_type: ACTION_TYPE.LOGIN, status: httpStatusOf(err), err },
            "Login failed",
          );
          throw err;
        }
      },
      signup: async (username, email, password) => {
        try {
          const data = await signupRequest(username, email, password);
          set({ user: data.user, token: data.token });
          log.info(
            { action_type: ACTION_TYPE.SIGNUP, status: 201, user_id: data.user.id },
            "Sign up succeeded",
          );
        } catch (err) {
          log.warn(
            { action_type: ACTION_TYPE.SIGNUP, status: httpStatusOf(err), err },
            "Sign up failed",
          );
          throw err;
        }
      },
      logout: () => {
        const user = get().user;
        set({ user: null, token: null });
        log.info(
          { action_type: ACTION_TYPE.LOGOUT, user_id: user?.id },
          "Logged out",
        );
      },
    }),
    {
      name: "auth-storage",
      partialize: (state) => ({ user: state.user, token: state.token }),
    },
  ),
);

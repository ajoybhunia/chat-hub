import axios from "axios";
import { useAuthStore } from "../store/auth.store";
import { API_PREFIX } from "../constants/paths";

/**
 * The single HTTP client for the app. All server calls go through this
 * instance (invariant: only src/services/ touches the network) — the browser
 * only ever talks to the BFF on its own origin.
 */
const api = axios.create({ baseURL: API_PREFIX });

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** HTTP status of a failed axios call, if any (undefined for network errors). */
export function httpStatusOf(err: unknown): number | undefined {
  return axios.isAxiosError(err) ? err.response?.status : undefined;
}

export default api;

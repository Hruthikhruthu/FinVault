import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { ApiResponse, AuthResponse } from "./types";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

interface AuthBridge {
  accessToken: () => string | null;
  refreshToken: () => string | null;
  setSession: (session: AuthResponse) => void;
  logout: () => void;
}

let bridge: AuthBridge | null = null;
let refreshPromise: Promise<string> | null = null;

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json"
  }
});

export function configureAuthBridge(nextBridge: AuthBridge) {
  bridge = nextBridge;
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = bridge?.accessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers["X-Device-Fingerprint"] = deviceFingerprint();
  return config;
});

api.interceptors.response.use(
  response => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    if (!bridge || !original || original._retry || error.response?.status !== 401) {
      return Promise.reject(error);
    }
    const refreshToken = bridge.refreshToken();
    if (!refreshToken) {
      bridge.logout();
      return Promise.reject(error);
    }
    original._retry = true;
    try {
      refreshPromise ??= axios
        .post<ApiResponse<AuthResponse>>(`${API_BASE_URL}/auth/refresh`, { refreshToken }, {
          headers: { "X-Device-Fingerprint": deviceFingerprint() }
        })
        .then(response => {
          bridge?.setSession(response.data.data);
          return response.data.data.accessToken;
        })
        .finally(() => {
          refreshPromise = null;
        });
      original.headers.Authorization = `Bearer ${await refreshPromise}`;
      return api(original);
    } catch (refreshError) {
      bridge.logout();
      return Promise.reject(refreshError);
    }
  }
);

export function unwrap<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data;
}

export function apiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error instanceof Error ? error.message : "Unexpected error";
}

export function deviceFingerprint() {
  const key = "finvault-device-fingerprint";
  const existing = localStorage.getItem(key);
  if (existing) {
    return existing;
  }
  const fingerprint = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
  localStorage.setItem(key, fingerprint);
  return fingerprint;
}

export function websocketUrl() {
  const base = API_BASE_URL.replace(/\/api\/?$/, "");
  if (base.startsWith("http")) {
    return `${base}/ws`;
  }
  return `${window.location.origin}${base}/ws`;
}

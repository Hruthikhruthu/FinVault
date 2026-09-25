import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useReducer } from "react";
import { api, configureAuthBridge, deviceFingerprint, unwrap } from "../api/client";
import type { ApiResponse, AuthResponse, Role, UserProfile } from "../api/types";

interface AuthUser {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  ready: boolean;
}

type AuthAction =
  | { type: "restore"; payload: Omit<AuthState, "ready"> }
  | { type: "session"; payload: AuthResponse }
  | { type: "profile"; payload: UserProfile }
  | { type: "logout" };

interface AuthContextValue extends AuthState {
  register: (fullName: string, email: string, password: string) => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  setSession: (session: AuthResponse) => void;
}

const STORAGE_KEY = "finvault-auth";
const AuthContext = createContext<AuthContextValue | null>(null);

function reducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case "restore":
      return { ...action.payload, ready: true };
    case "session":
      return {
        accessToken: action.payload.accessToken,
        refreshToken: action.payload.refreshToken,
        ready: true,
        user: {
          id: action.payload.userId,
          email: action.payload.email,
          fullName: action.payload.email.split("@")[0],
          role: action.payload.role
        }
      };
    case "profile":
      return {
        ...state,
        user: {
          id: action.payload.id,
          email: action.payload.email,
          fullName: action.payload.fullName,
          role: action.payload.role
        }
      };
    case "logout":
      return { accessToken: null, refreshToken: null, user: null, ready: true };
    default:
      return state;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, {
    accessToken: null,
    refreshToken: null,
    user: null,
    ready: false
  });

  const setSession = useCallback((session: AuthResponse) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    dispatch({ type: "session", payload: session });
  }, []);

  const logout = useCallback(() => {
    const refreshToken = localStorage.getItem(STORAGE_KEY);
    if (refreshToken) {
      const stored = JSON.parse(refreshToken) as AuthResponse;
      api.post("/auth/logout", { refreshToken: stored.refreshToken }).catch(() => undefined);
    }
    localStorage.removeItem(STORAGE_KEY);
    dispatch({ type: "logout" });
  }, []);

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) {
      dispatch({ type: "restore", payload: { accessToken: null, refreshToken: null, user: null } });
      return;
    }
    const session = JSON.parse(stored) as AuthResponse;
    dispatch({ type: "session", payload: session });
  }, []);

  useEffect(() => {
    configureAuthBridge({
      accessToken: () => state.accessToken,
      refreshToken: () => state.refreshToken,
      setSession,
      logout
    });
  }, [logout, setSession, state.accessToken, state.refreshToken]);

  useEffect(() => {
    if (!state.accessToken) {
      return;
    }
    api.get<ApiResponse<UserProfile>>("/auth/me")
      .then(response => dispatch({ type: "profile", payload: unwrap(response) }))
      .catch(() => undefined);
  }, [state.accessToken]);

  const register = useCallback(async (fullName: string, email: string, password: string) => {
    const response = await api.post<ApiResponse<AuthResponse>>("/auth/register", { fullName, email, password });
    setSession(unwrap(response));
  }, [setSession]);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.post<ApiResponse<AuthResponse>>("/auth/login", {
      email,
      password,
      deviceFingerprint: deviceFingerprint()
    });
    setSession(unwrap(response));
  }, [setSession]);

  const value = useMemo<AuthContextValue>(() => ({
    ...state,
    register,
    login,
    logout,
    setSession
  }), [login, logout, register, setSession, state]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}

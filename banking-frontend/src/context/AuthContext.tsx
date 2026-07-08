import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import { api, setAuthToken, setUnauthorizedHandler } from "../api/client";
import type { AuthSession, MeProfile } from "../types";

const STORAGE_KEY = "banking_session";

interface AuthContextValue {
  session: AuthSession | null;
  profile: MeProfile | null;
  loading: boolean;
  login: (session: AuthSession) => void;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadSession(): AuthSession | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const session = JSON.parse(raw) as AuthSession;
    if (session.token) {
      setAuthToken(session.token);
    }
    return session;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [session, setSession] = useState<AuthSession | null>(loadSession);
  const [profile, setProfile] = useState<MeProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const clearSession = useCallback(() => {
    setAuthToken(null);
    sessionStorage.removeItem(STORAGE_KEY);
    setSession(null);
    setProfile(null);
  }, []);

  const login = useCallback((next: AuthSession) => {
    setAuthToken(next.token);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    setSession(next);
  }, []);

  const logout = useCallback(() => {
    void api.logout().catch(() => {
      /* ignore network errors on sign-out */
    });
    clearSession();
  }, [clearSession]);

  const refreshProfile = useCallback(async () => {
    try {
      const me = await api.me();
      setProfile(me);
    } catch {
      /* handled by unauthorized handler if it was an auth failure */
    }
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSession();
      navigate("/login", { replace: true });
    });
    return () => setUnauthorizedHandler(null);
  }, [clearSession, navigate]);

  useEffect(() => {
    let cancelled = false;
    async function hydrate() {
      if (!session) {
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const me = await api.me();
        if (!cancelled) setProfile(me);
      } catch {
        /* onUnauthorized handles token invalidation; ignore other errors here */
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void hydrate();
    return () => {
      cancelled = true;
    };
    // Only re-hydrate when the session identity changes (login/logout), not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.token]);

  const value = useMemo(
    () => ({ session, profile, loading, login, logout, refreshProfile }),
    [session, profile, loading, login, logout, refreshProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

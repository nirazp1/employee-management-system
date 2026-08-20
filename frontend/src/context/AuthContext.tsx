import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api } from "../lib/api";
import { tokenStorage } from "../lib/tokenStorage";
import type { LoginResponse, RoleName, UserResponse } from "../lib/types";

export { isApiError } from "../lib/api";

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: RoleName[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  // Starts true: on a hard refresh we only know a token *might* be sitting in
  // localStorage, so every route has to wait for the /auth/me check before
  // deciding whether to bounce to /login.
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const accessToken = tokenStorage.getAccessToken();
    if (!accessToken) {
      setLoading(false);
      return;
    }
    api
      .get<UserResponse>("/auth/me")
      .then(setUser)
      .catch(() => tokenStorage.clear())
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const result = await api.post<LoginResponse>("/auth/login", { email, password });
    tokenStorage.setTokens(result.accessToken, result.refreshToken);
    setUser(result.user);
  }, []);

  const register = useCallback(
    async (email: string, password: string, firstName: string, lastName: string) => {
      await api.post<UserResponse>("/auth/register", { email, password, firstName, lastName });
      // Registration doesn't return tokens (it's not a session, just an account
      // creation) - log in right after so the UX is a single "create account" step.
      await login(email, password);
    },
    [login],
  );

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (...roles: RoleName[]) => !!user && roles.some((role) => user.roles.includes(role)),
    [user],
  );

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

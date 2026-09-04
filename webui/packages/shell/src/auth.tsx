import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { ACCESS_TOKEN_EXPIRY_KEY, ACCESS_TOKEN_KEY, login as loginRequest, setUnauthorizedHandler } from '@api';
import type { LoginRequest, LoginResponse } from '@domain';

export type Account = Pick<LoginResponse, 'accountId' | 'email' | 'fullName' | 'roles'>;

export type CreateAuthOptions = {
  /** localStorage key for the persisted account (per app, so consoles don't share sessions). */
  accountKey: string;
  /** Where RequireAuth redirects unauthenticated users. Defaults to '/login'. */
  loginPath?: string;
  /** Optional role gate; when it returns false, signIn throws `deniedError()` and no session is stored. */
  hasAccess?: (roles: string[]) => boolean;
  deniedError?: () => Error;
};

export type AuthContextValue = {
  account: Account | null;
  isAuthenticated: boolean;
  signIn: (credentials: LoginRequest) => Promise<void>;
  signOut: () => void;
};

/**
 * Builds an app-scoped auth context (Provider + hook + route guard). Both admin apps share this
 * logic; they differ only by storage key and an optional role gate passed in here.
 */
export function createAuth(options: CreateAuthOptions) {
  const { accountKey, loginPath = '/login', hasAccess, deniedError } = options;
  const AuthContext = createContext<AuthContextValue | null>(null);

  const clearSession = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(ACCESS_TOKEN_EXPIRY_KEY);
    localStorage.removeItem(accountKey);
  };

  const readValidAccount = (): Account | null => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    const expiresAt = Number(localStorage.getItem(ACCESS_TOKEN_EXPIRY_KEY));
    const raw = localStorage.getItem(accountKey);
    if (!token || !raw || !expiresAt || Date.now() >= expiresAt) {
      clearSession();
      return null;
    }
    try {
      const account = JSON.parse(raw) as Account;
      if (hasAccess && !hasAccess(account.roles)) {
        clearSession();
        return null;
      }
      return account;
    } catch {
      clearSession();
      return null;
    }
  };

  function AuthProvider({ children }: { children: ReactNode }) {
    const [account, setAccount] = useState<Account | null>(() => readValidAccount());

    const signOut = useCallback(() => {
      clearSession();
      setAccount(null);
    }, []);

    const signIn = useCallback(async (credentials: LoginRequest) => {
      const response = await loginRequest(credentials);
      if (hasAccess && !hasAccess(response.roles)) {
        clearSession();
        throw deniedError ? deniedError() : new Error('Access denied.');
      }
      const next: Account = {
        accountId: response.accountId,
        email: response.email,
        fullName: response.fullName,
        roles: response.roles
      };
      localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
      localStorage.setItem(ACCESS_TOKEN_EXPIRY_KEY, String(Date.now() + response.expiresInSeconds * 1000));
      localStorage.setItem(accountKey, JSON.stringify(next));
      setAccount(next);
    }, []);

    // Backend rejecting the token (401) drops the session so the guard redirects to login.
    useEffect(() => {
      setUnauthorizedHandler(signOut);
      return () => setUnauthorizedHandler(null);
    }, [signOut]);

    // Expire the session in-place when the token's lifetime runs out mid-session.
    useEffect(() => {
      const expiresAt = Number(localStorage.getItem(ACCESS_TOKEN_EXPIRY_KEY));
      if (!account || !expiresAt) return;
      const timeout = window.setTimeout(signOut, Math.max(expiresAt - Date.now(), 0));
      return () => window.clearTimeout(timeout);
    }, [account, signOut]);

    const value = useMemo<AuthContextValue>(
      () => ({ account, isAuthenticated: Boolean(account), signIn, signOut }),
      [account, signIn, signOut]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
  }

  function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within its AuthProvider');
    return context;
  }

  function RequireAuth() {
    const { isAuthenticated } = useAuth();
    const location = useLocation();
    if (!isAuthenticated) {
      return <Navigate to={loginPath} replace state={{ from: location.pathname }} />;
    }
    return <Outlet />;
  }

  return { AuthProvider, useAuth, RequireAuth };
}

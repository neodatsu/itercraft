import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';
import type Keycloak from 'keycloak-js';
import { keycloak } from './keycloak';

interface AuthContextType {
  keycloak: Keycloak;
  authenticated: boolean;
  initialized: boolean;
  ensureInit: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const initPromise = useRef<Promise<boolean> | null>(null);

  const ensureInit = useCallback(() => {
    initPromise.current ??= keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    }).then((auth) => {
      setAuthenticated(auth);
      setInitialized(true);
      return auth;
    });
    return initPromise.current;
  }, []);

  const value = useMemo(
    () => ({ keycloak, authenticated, initialized, ensureInit }),
    [authenticated, initialized, ensureInit]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

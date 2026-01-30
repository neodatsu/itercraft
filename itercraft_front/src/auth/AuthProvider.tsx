import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type Keycloak from 'keycloak-js';
import { keycloak } from './keycloak';

interface AuthContextType {
  keycloak: Keycloak;
  authenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    }).then((auth) => {
      setAuthenticated(auth);
      setInitialized(true);
    });
  }, []);

  if (!initialized) return null;

  return (
    <AuthContext.Provider value={{ keycloak, authenticated }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

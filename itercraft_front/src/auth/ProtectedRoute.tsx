import { useEffect, type ReactNode } from 'react';
import { useAuth } from './AuthProvider';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { keycloak, authenticated, initialized, ensureInit } = useAuth();

  useEffect(() => {
    ensureInit();
  }, [ensureInit]);

  if (!initialized) return null;

  if (!authenticated) {
    keycloak.login({ redirectUri: `${window.location.origin}/dashboard` });
    return null;
  }

  return <>{children}</>;
}

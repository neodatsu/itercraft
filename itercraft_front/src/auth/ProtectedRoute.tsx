import { type ReactNode } from 'react';
import { useAuth } from './AuthProvider';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { keycloak, authenticated } = useAuth();

  if (!authenticated) {
    keycloak.login();
    return null;
  }

  return <>{children}</>;
}

import { useAuth } from '../../auth/AuthProvider';
import './DashboardPage.css';

export function DashboardPage() {
  const { keycloak } = useAuth();
  const name = keycloak.tokenParsed?.preferred_username
    ?? keycloak.tokenParsed?.name
    ?? 'User';

  return (
    <div className="dashboard-container">
      <h1>Dashboard</h1>
      <p className="dashboard-welcome">Welcome, <strong>{name}</strong></p>
    </div>
  );
}

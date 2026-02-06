import { useAuth } from '../../auth/AuthProvider';
import './DashboardPage.css';

export function DashboardPage() {
  const { keycloak } = useAuth();
  const name = keycloak.tokenParsed?.preferred_username
    ?? keycloak.tokenParsed?.name
    ?? 'Utilisateur';

  return (
    <div className="dashboard-container">
      <h1>Tableau de bord</h1>
      <p className="dashboard-welcome">Bienvenue, <strong>{name}</strong></p>
    </div>
  );
}

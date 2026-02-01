import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthProvider';
import './Header.css';

export function Header() {
  const { keycloak, authenticated } = useAuth();

  return (
    <header className="app-header">
      <Link to="/" className="app-header-logo">itercraft</Link>
      <nav className="app-header-nav">
        {authenticated ? (
          <>
            <Link to="/dashboard" className="app-header-link">Tableau de bord</Link>
            <button
              className="app-header-button"
              onClick={() => keycloak.logout({ redirectUri: window.location.origin })}
            >
              Déconnexion
            </button>
          </>
        ) : (
          <Link to="/dashboard" className="app-header-button">Connexion</Link>
        )}
      </nav>
    </header>
  );
}

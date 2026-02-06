import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthProvider';
import './Header.css';

export function Header() {
  const { keycloak, authenticated } = useAuth();

  return (
    <header className="app-header">
      <Link to="/" className="app-header-logo">itercraft</Link>
      <nav className="app-header-nav">
        <Link to="/architecture" className="app-header-link">Architecture</Link>
        <Link to="/sse" className="app-header-link">SSE</Link>
        {authenticated ? (
          <>
            <Link to="/dashboard" className="app-header-link">Tableau de bord</Link>
            <Link to="/entretien" className="app-header-link">Entretien</Link>
            <Link to="/activites" className="app-header-link">Activités</Link>
            <Link to="/ludotheque" className="app-header-link">Ma Ludothèque</Link>
            <button
              className="app-header-button"
              onClick={() => keycloak.logout({ redirectUri: globalThis.location.origin })}
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

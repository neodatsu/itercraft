import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthProvider';
import './Header.css';

export function Header() {
  const { keycloak, authenticated } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="app-header">
      <Link to="/" className="app-header-logo" onClick={closeMenu}>itercraft</Link>
      <button
        className="app-header-burger"
        aria-label="Menu de navigation"
        aria-expanded={menuOpen}
        onClick={() => setMenuOpen(prev => !prev)}
      >
        {menuOpen ? '\u2715' : '\u2630'}
      </button>
      <nav className={`app-header-nav${menuOpen ? ' open' : ''}`}>
        <Link to="/architecture" className="app-header-link" onClick={closeMenu}>Architecture</Link>
        <Link to="/sse" className="app-header-link" onClick={closeMenu}>SSE</Link>
        <Link to="/resilience" className="app-header-link" onClick={closeMenu}>Résilience</Link>
        <Link to="/iot" className="app-header-link" onClick={closeMenu}>IoT</Link>
        {authenticated ? (
          <>
            <Link to="/dashboard" className="app-header-link" onClick={closeMenu}>Tableau de bord</Link>
            <Link to="/entretien" className="app-header-link" onClick={closeMenu}>Entretien</Link>
            <Link to="/activites" className="app-header-link" onClick={closeMenu}>Activités</Link>
            <Link to="/ludotheque" className="app-header-link" onClick={closeMenu}>Ma Ludothèque</Link>
            <button
              className="app-header-button"
              onClick={() => { closeMenu(); keycloak.logout({ redirectUri: globalThis.location.origin }); }}
            >
              Déconnexion
            </button>
          </>
        ) : (
          <Link to="/dashboard" className="app-header-button" onClick={closeMenu}>Connexion</Link>
        )}
      </nav>
    </header>
  );
}

import { Link } from 'react-router-dom';
import './Header.css';

export function PublicHeader() {
  return (
    <header className="app-header">
      <Link to="/" className="app-header-logo">itercraft</Link>
      <nav className="app-header-nav">
        <Link to="/dashboard" className="app-header-button">Login</Link>
      </nav>
    </header>
  );
}

import { Link } from 'react-router-dom';
import './Footer.css';

export function Footer() {
  return (
    <footer className="app-footer">
      <span>&copy; {new Date().getFullYear()} Itercraft</span>
      <Link to="/cookies" className="app-footer-link">Politique de cookies</Link>
    </footer>
  );
}

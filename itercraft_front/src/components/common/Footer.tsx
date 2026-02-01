import { Link } from 'react-router-dom';
import './Footer.css';

export function Footer() {
  return (
    <footer className="app-footer">
      <span>&copy; {new Date().getFullYear()} Itercraft</span>
      <Link to="/mentions-legales" className="app-footer-link">Mentions légales</Link>
      <Link to="/confidentialite" className="app-footer-link">Confidentialité</Link>
      <Link to="/cookies" className="app-footer-link">Cookies</Link>
    </footer>
  );
}

import { Link, useLocation } from 'react-router-dom';
import './Breadcrumb.css';

const labels: Record<string, string> = {
  '/': 'Accueil',
  '/dashboard': 'Tableau de bord',
  '/cookies': 'Politique de cookies',
  '/mentions-legales': 'Mentions légales',
  '/confidentialite': 'Confidentialité',
  '/architecture': 'Architecture',
  '/sse': 'Server-Sent Events',
  '/meteo': 'Météo',
};

export function Breadcrumb() {
  const { pathname } = useLocation();

  if (pathname === '/') return null;

  const segments = pathname.split('/').filter(Boolean);
  const crumbs = [{ path: '/', label: 'Accueil' }];

  let current = '';
  for (const seg of segments) {
    current += `/${seg}`;
    crumbs.push({ path: current, label: labels[current] ?? seg });
  }

  return (
    <nav className="breadcrumb" aria-label="Breadcrumb">
      {crumbs.map((crumb, i) => (
        <span key={crumb.path}>
          {i > 0 && <span className="breadcrumb-sep">/</span>}
          {i === crumbs.length - 1 ? (
            <span className="breadcrumb-current">{crumb.label}</span>
          ) : (
            <Link to={crumb.path} className="breadcrumb-link">{crumb.label}</Link>
          )}
        </span>
      ))}
    </nav>
  );
}

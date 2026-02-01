import { useState } from 'react';
import { Link } from 'react-router-dom';
import './CookieConsent.css';

const STORAGE_KEY = 'cookie-consent';

export function CookieConsent() {
  const [visible, setVisible] = useState(() => !localStorage.getItem(STORAGE_KEY));

  function handleChoice(choice: 'accepted' | 'refused') {
    localStorage.setItem(STORAGE_KEY, choice);
    setVisible(false);
  }

  if (!visible) return null;

  return (
    <div className="cookie-consent" role="banner" aria-label="Cookie consent">
      <p>
        Ce site utilise des cookies pour son fonctionnement.{' '}
        <Link to="/cookies">Consultez notre politique de cookies</Link>.
      </p>
      <div className="cookie-consent-buttons">
        <button className="btn btn-primary btn-sm" onClick={() => handleChoice('accepted')}>
          Accepter
        </button>
        <button className="btn btn-secondary btn-sm" onClick={() => handleChoice('refused')}>
          Refuser
        </button>
      </div>
    </div>
  );
}

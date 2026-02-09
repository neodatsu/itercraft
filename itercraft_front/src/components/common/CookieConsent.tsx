import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import './CookieConsent.css';

const STORAGE_KEY = 'cookie-consent';

declare global {
  // eslint-disable-next-line no-var
  var gtag: ((...args: unknown[]) => void) | undefined;
}

function grantAnalyticsConsent() {
  globalThis.gtag?.('consent', 'update', {
    analytics_storage: 'granted',
  });
}

export function CookieConsent() {
  const [visible, setVisible] = useState(() => !localStorage.getItem(STORAGE_KEY));

  useEffect(() => {
    if (localStorage.getItem(STORAGE_KEY) === 'accepted') {
      grantAnalyticsConsent();
    }
  }, []);

  function handleChoice(choice: 'accepted' | 'refused') {
    localStorage.setItem(STORAGE_KEY, choice);
    setVisible(false);
    if (choice === 'accepted') {
      grantAnalyticsConsent();
    }
  }

  if (!visible) return null;

  return (
    <section className="cookie-consent" aria-label="Cookie consent">
      <p>
        Ce site utilise des cookies techniques et de mesure d'audience.{' '}
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
    </section>
  );
}

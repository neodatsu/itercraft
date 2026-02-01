import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import './CookieConsent.css';

const STORAGE_KEY = 'cookie-consent';
const GA_ID = 'G-NMSXHLBJZK';

function loadGoogleAnalytics() {
  if (document.getElementById('ga-script')) return;
  const script = document.createElement('script');
  script.id = 'ga-script';
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_ID}`;
  document.head.appendChild(script);

  window.dataLayer = window.dataLayer || [];
  function gtag(...args: unknown[]) { window.dataLayer!.push(args); }
  gtag('js', new Date());
  gtag('config', GA_ID);
}

declare global {
  interface Window {
    dataLayer?: unknown[];
  }
}

export function CookieConsent() {
  const [visible, setVisible] = useState(() => !localStorage.getItem(STORAGE_KEY));

  useEffect(() => {
    if (localStorage.getItem(STORAGE_KEY) === 'accepted') {
      loadGoogleAnalytics();
    }
  }, []);

  function handleChoice(choice: 'accepted' | 'refused') {
    localStorage.setItem(STORAGE_KEY, choice);
    setVisible(false);
    if (choice === 'accepted') {
      loadGoogleAnalytics();
    }
  }

  if (!visible) return null;

  return (
    <div className="cookie-consent" role="banner" aria-label="Cookie consent">
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
    </div>
  );
}

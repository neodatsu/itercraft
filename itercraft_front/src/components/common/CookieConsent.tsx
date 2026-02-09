import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import './CookieConsent.css';

const STORAGE_KEY = 'cookie-consent';
const GA_ID = 'G-NMSXHLBJZK';

declare global {
  // eslint-disable-next-line no-var
  var dataLayer: unknown[] | undefined;
}

function gtag(...args: unknown[]) {
  globalThis.dataLayer = globalThis.dataLayer ?? [];
  globalThis.dataLayer.push(args);
}

// Set default consent to denied BEFORE loading gtag.js (Consent Mode v2)
gtag('consent', 'default', {
  analytics_storage: 'denied',
  ad_storage: 'denied',
  ad_user_data: 'denied',
  ad_personalization: 'denied',
});

function loadGtagScript() {
  if (document.getElementById('ga-script')) return;

  gtag('js', new Date());
  gtag('config', GA_ID);

  const script = document.createElement('script');
  script.id = 'ga-script';
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_ID}`;
  document.head.appendChild(script);
}

function grantAnalyticsConsent() {
  gtag('consent', 'update', {
    analytics_storage: 'granted',
  });
}

export function CookieConsent() {
  const [visible, setVisible] = useState(() => !localStorage.getItem(STORAGE_KEY));

  useEffect(() => {
    loadGtagScript();
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

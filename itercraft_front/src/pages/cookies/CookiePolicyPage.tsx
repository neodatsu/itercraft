import './CookiePolicyPage.css';

export function CookiePolicyPage() {
  return (
    <div className="cookie-policy">
      <h1>Politique de cookies</h1>
      <p className="cookie-policy-intro">
        Conformément à la réglementation française (loi Informatique et Libertés, directive ePrivacy
        et recommandations de la CNIL), nous vous informons de l'utilisation de cookies et
        technologies similaires sur l'ensemble de nos services. Les cookies listés ci-dessous sont
        strictement nécessaires au fonctionnement technique du site et ne requièrent pas de
        consentement au titre de l'article 82 de la loi Informatique et Libertés. Aucun cookie de
        traçage publicitaire ou analytique n'est utilisé.
      </p>

      <h2>Cookies du domaine www (application front-end)</h2>
      <table>
        <thead>
          <tr>
            <th>Cookie</th>
            <th>Finalité</th>
            <th>Durée</th>
            <th>Responsable</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>cookie-consent</code></td>
            <td>Mémorisation du choix de l'utilisateur concernant les cookies (localStorage)</td>
            <td>Permanent</td>
            <td>Itercraft</td>
          </tr>
        </tbody>
      </table>

      <h2>Cookies du domaine authent (Keycloak — authentification)</h2>
      <table>
        <thead>
          <tr>
            <th>Cookie</th>
            <th>Finalité</th>
            <th>Durée</th>
            <th>Responsable</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>AUTH_SESSION_ID</code></td>
            <td>Identifiant de session d'authentification</td>
            <td>Session</td>
            <td>
              Keycloak —{' '}
              <a href="https://www.keycloak.org/docs/latest/server_admin/#_cookies" target="_blank" rel="noopener noreferrer">
                Politique cookies Keycloak
              </a>
            </td>
          </tr>
          <tr>
            <td><code>KEYCLOAK_SESSION</code></td>
            <td>Maintien de la session SSO (Single Sign-On)</td>
            <td>Session</td>
            <td>Keycloak</td>
          </tr>
          <tr>
            <td><code>KEYCLOAK_IDENTITY</code></td>
            <td>Identité de l'utilisateur authentifié</td>
            <td>Session</td>
            <td>Keycloak</td>
          </tr>
          <tr>
            <td><code>KC_RESTART</code></td>
            <td>Reprise d'un flux d'authentification interrompu</td>
            <td>Session</td>
            <td>Keycloak</td>
          </tr>
        </tbody>
      </table>

      <h2>Cookies du domaine api (backend Spring Boot)</h2>
      <table>
        <thead>
          <tr>
            <th>Cookie</th>
            <th>Finalité</th>
            <th>Durée</th>
            <th>Responsable</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>XSRF-TOKEN</code></td>
            <td>Protection contre les attaques CSRF (Cross-Site Request Forgery)</td>
            <td>Session</td>
            <td>
              Spring Security —{' '}
              <a href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html" target="_blank" rel="noopener noreferrer">
                Documentation CSRF
              </a>
            </td>
          </tr>
        </tbody>
      </table>

      <div className="cookie-policy-legal">
        <p>
          <strong>Responsable du traitement :</strong> Itercraft
        </p>
        <p>
          <strong>Base légale :</strong> Intérêt légitime — les cookies listés ci-dessus sont
          strictement nécessaires à la fourniture du service demandé par l'utilisateur (exemption de
          consentement au titre de l'article 82 de la loi Informatique et Libertés).
        </p>
        <p>
          <strong>Vos droits :</strong> Vous pouvez à tout moment supprimer les cookies via les
          paramètres de votre navigateur. La suppression des cookies de session entraînera une
          déconnexion.
        </p>
      </div>
    </div>
  );
}

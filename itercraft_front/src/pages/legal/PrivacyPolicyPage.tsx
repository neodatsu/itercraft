import { Link } from 'react-router-dom';
import './legal.css';

export function PrivacyPolicyPage() {
  return (
    <div className="legal-page">
      <h1>Politique de confidentialité</h1>
      <p className="legal-intro">
        Conformément au Règlement Général sur la Protection des Données (RGPD - UE 2016/679) et à la
        loi Informatique et Libertés du 6 janvier 1978 modifiée, cette politique décrit comment
        Itercraft collecte, utilise et protège vos données personnelles.
      </p>

      <h2>Responsable du traitement</h2>
      <p>
        Itercraft est responsable du traitement des données personnelles collectées sur le site
        itercraft.com et ses sous-domaines.
      </p>

      <h2>Données collectées</h2>
      <table>
        <thead>
          <tr>
            <th>Donnée</th>
            <th>Finalité</th>
            <th>Base légale</th>
            <th>Durée de conservation</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Identifiant Keycloak (sub)</td>
            <td>Authentification et gestion du compte</td>
            <td>Exécution du contrat</td>
            <td>Durée du compte</td>
          </tr>
          <tr>
            <td>Nom d'utilisateur</td>
            <td>Affichage dans l'interface</td>
            <td>Exécution du contrat</td>
            <td>Durée du compte</td>
          </tr>
          <tr>
            <td>Abonnements aux services</td>
            <td>Gestion des souscriptions</td>
            <td>Exécution du contrat</td>
            <td>Durée du compte</td>
          </tr>
          <tr>
            <td>Historique d'utilisation</td>
            <td>Suivi des usages par service</td>
            <td>Exécution du contrat</td>
            <td>Durée du compte</td>
          </tr>
          <tr>
            <td>Données de navigation (Google Analytics)</td>
            <td>Mesure d'audience et amélioration du service</td>
            <td>Consentement</td>
            <td>26 mois</td>
          </tr>
        </tbody>
      </table>

      <h2>Sous-traitants</h2>
      <table>
        <thead>
          <tr>
            <th>Sous-traitant</th>
            <th>Service</th>
            <th>Localisation</th>
            <th>Politique de confidentialité</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Amazon Web Services (AWS)</td>
            <td>Hébergement (EC2, ECR)</td>
            <td>Irlande (eu-west-1)</td>
            <td>
              <a href="https://aws.amazon.com/privacy/" target="_blank" rel="noopener noreferrer">
                aws.amazon.com/privacy
              </a>
            </td>
          </tr>
          <tr>
            <td>Cloudflare</td>
            <td>DNS, proxy HTTPS, protection DDoS</td>
            <td>États-Unis / UE</td>
            <td>
              <a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener noreferrer">
                cloudflare.com/privacypolicy
              </a>
            </td>
          </tr>
          <tr>
            <td>Google (Google Analytics)</td>
            <td>Mesure d'audience</td>
            <td>États-Unis / UE</td>
            <td>
              <a href="https://policies.google.com/privacy" target="_blank" rel="noopener noreferrer">
                policies.google.com/privacy
              </a>
            </td>
          </tr>
        </tbody>
      </table>

      <h2>Transferts hors UE</h2>
      <p>
        Certaines données peuvent être traitées aux États-Unis par Cloudflare et Google. Ces
        transferts sont encadrés par le Data Privacy Framework (DPF) UE-États-Unis ou, le cas
        échéant, par des clauses contractuelles types approuvées par la Commission européenne.
      </p>

      <h2>Vos droits</h2>
      <p>Conformément au RGPD, vous disposez des droits suivants :</p>
      <ul>
        <li><strong>Droit d'accès</strong> : obtenir la confirmation que des données vous concernant sont traitées et en obtenir une copie.</li>
        <li><strong>Droit de rectification</strong> : demander la correction de données inexactes ou incomplètes.</li>
        <li><strong>Droit à l'effacement</strong> : demander la suppression de vos données dans les conditions prévues par le RGPD.</li>
        <li><strong>Droit à la limitation</strong> : demander la limitation du traitement de vos données.</li>
        <li><strong>Droit à la portabilité</strong> : recevoir vos données dans un format structuré et couramment utilisé.</li>
        <li><strong>Droit d'opposition</strong> : vous opposer au traitement de vos données pour des motifs légitimes.</li>
        <li><strong>Retrait du consentement</strong> : retirer à tout moment votre consentement pour les traitements fondés sur celui-ci (Google Analytics).</li>
      </ul>
      <p>
        Pour exercer ces droits, vous pouvez nous contacter à l'adresse indiquée dans les{' '}
        <Link to="/mentions-legales">mentions légales</Link>.
      </p>

      <h2>Sécurité</h2>
      <p>
        Itercraft met en œuvre les mesures techniques et organisationnelles appropriées pour protéger
        vos données : chiffrement HTTPS (TLS) via Cloudflare, authentification OAuth2/OIDC avec
        PKCE via Keycloak, protection CSRF, analyse de sécurité OWASP et SonarCloud.
      </p>

      <h2>Cookies</h2>
      <p>
        Pour les détails sur les cookies utilisés, consultez notre{' '}
        <Link to="/cookies">Politique de cookies</Link>.
      </p>

      <h2>Réclamation</h2>
      <p>
        Si vous estimez que le traitement de vos données ne respecte pas la réglementation, vous
        pouvez introduire une réclamation auprès de la CNIL :{' '}
        <a href="https://www.cnil.fr" target="_blank" rel="noopener noreferrer">www.cnil.fr</a>.
      </p>

      <h2>Mise à jour</h2>
      <p>
        Cette politique de confidentialité peut être mise à jour à tout moment. La date de dernière
        modification est indiquée ci-dessous.
      </p>
      <p><strong>Dernière mise à jour :</strong> 1er février 2026.</p>
    </div>
  );
}

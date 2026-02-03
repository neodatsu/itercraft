import { useEffect, useRef } from 'react';
import mermaid from 'mermaid';
import './ArchitecturePage.css';

const contextDiagram = `
C4Context
  title Itercraft — Diagramme de contexte (C4 Level 1)

  Person(user, "Utilisateur", "Propriétaire de maison utilisant les services Itercraft")

  System(itercraft, "Itercraft", "Application web de gestion d'abonnements et suivi d'usage de services")

  System_Ext(keycloak, "Keycloak 26", "Fournisseur d'identité OAuth2/OIDC")
  System_Ext(cloudflare, "Cloudflare", "DNS, proxy HTTPS, protection DDoS")
  System_Ext(aws, "AWS", "Hébergement cloud (EC2, ECR, S3, DynamoDB, Lambda, API Gateway)")
  System_Ext(github, "GitHub Actions", "Pipeline CI/CD, tests, analyse de sécurité")
  System_Ext(sonar, "SonarCloud", "Analyse qualité et couverture de code")
  System_Ext(ga, "Google Analytics", "Mesure d'audience (consentement RGPD)")
  System_Ext(slack, "Slack", "Notifications CI/CD + ChatOps /infra")
  System_Ext(meteofrance, "Météo France", "API AROME PI (cartes WMS)")
  System_Ext(claude, "Claude API", "API Anthropic (analyse d'images météo)")

  Rel(user, itercraft, "Utilise", "HTTPS")
  Rel(itercraft, keycloak, "Authentification", "OAuth2/OIDC PKCE")
  Rel(cloudflare, itercraft, "Proxy", "HTTPS")
  Rel(github, itercraft, "Déploie", "Docker / ECR")
  Rel(github, sonar, "Analyse", "API")
  Rel(github, slack, "Notifications", "Webhook")
  Rel(slack, github, "ChatOps /infra", "Lambda → workflow_dispatch")
  Rel(itercraft, ga, "Envoie métriques", "HTTPS")
  Rel(itercraft, aws, "Hébergé sur", "EC2")
  Rel(itercraft, meteofrance, "Cartes météo", "HTTPS")
  Rel(itercraft, claude, "Analyse météo", "HTTPS")
`;

const containerDiagram = `
C4Container
  title Itercraft — Diagramme de conteneurs (C4 Level 2)

  Person(user, "Utilisateur", "Navigateur web")

  System_Boundary(itercraft, "Itercraft") {
    Container(traefik, "Traefik v3", "Reverse Proxy", "Routage HTTP : / → front, /api → back, /auth → Keycloak")
    Container(front, "Frontend", "React 19, Vite, TypeScript", "SPA : dashboard, gestion d'abonnements, SSE temps réel")
    Container(api, "Backend API", "Java 25, Spring Boot 4.0.2", "REST API, SSE, OAuth2 Resource Server, CSRF")
    ContainerDb(db, "PostgreSQL 17", "Base de données", "Utilisateurs, services, abonnements, usages. Migrations Liquibase")
    Container(kc, "Keycloak 26", "Serveur d'identité", "Realm itercraft, client iterfront, OIDC PKCE")
    Container(prom, "Prometheus", "Monitoring", "Collecte métriques /actuator/prometheus")
    Container(grafana, "Grafana", "Tableaux de bord", "Visualisation des métriques applicatives")
  }

  System_Ext(claude2, "Claude API", "API Anthropic (analyse vision)")
  System_Ext(meteofrance2, "Météo France", "API AROME PI (cartes WMS)")

  Rel(user, traefik, "HTTPS", "443")
  Rel(traefik, front, "Proxy", "/")
  Rel(traefik, api, "Proxy", "/api")
  Rel(traefik, kc, "Proxy", "/auth")
  Rel(traefik, grafana, "Proxy", "/grafana")
  Rel(front, api, "API REST + SSE", "HTTPS /api")
  Rel(front, kc, "Auth OIDC", "HTTPS /auth")
  Rel(api, db, "JDBC", "5432")
  Rel(api, kc, "Validation token", "HTTP")
  Rel(prom, api, "Scrape métriques", "/actuator/prometheus")
  Rel(grafana, prom, "Requêtes", "PromQL")
  Rel(api, claude2, "Analyse image", "HTTPS")
  Rel(api, meteofrance2, "Cartes WMS", "HTTPS")
`;

mermaid.initialize({ startOnLoad: false, theme: 'default' });

export function ArchitecturePage() {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current) {
      mermaid.run({ nodes: containerRef.current.querySelectorAll('.mermaid-diagram') });
    }
  }, []);

  return (
    <div className="architecture-container" ref={containerRef}>
      <h1>Architecture</h1>

      <section className="architecture-section">
        <h2>Contexte (C4 Level 1)</h2>
        <pre className="mermaid-diagram">{contextDiagram}</pre>
      </section>

      <section className="architecture-section">
        <h2>Conteneurs (C4 Level 2)</h2>
        <pre className="mermaid-diagram">{containerDiagram}</pre>
      </section>
    </div>
  );
}

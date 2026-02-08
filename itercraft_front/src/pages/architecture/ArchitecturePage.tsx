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
  System_Ext(claude, "Claude API", "API Anthropic (suggestions d'activités)")

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
  Rel(itercraft, claude, "Suggestions activités", "HTTPS")
`;

const iotDiagram = `
C4Container
  title Itercraft — IoT Architecture (C4 Level 2)

  Person(homeowner, "Propriétaire", "Utilisateur avec objets connectés")

  System_Boundary(home, "Maison") {
    Container(esp32, "ESP32", "Microcontrôleur", "Capteurs : température, humidité, luminosité")
    Container(sensors, "Capteurs", "GPIO", "DHT22, photorésistance, barrière IR")
  }

  System_Boundary(itercraft_iot, "Itercraft IoT") {
    Container(mosquitto, "Mosquitto", "MQTT Broker", "TLS 1.3, auth par mot de passe, ACL")
    Container(api2, "Backend API", "Spring Boot", "Spring Integration MQTT, subscribe sensors/#")
    ContainerDb(db2, "PostgreSQL", "Base de données", "sensor_device, sensor_data")
    Container(dashboard, "Dashboard", "React + recharts", "Graphiques temps réel, filtre dates, SSE")
  }

  System_Boundary(dns, "DNS & Sécurité") {
    Container(cf_dns, "Cloudflare DNS", "DNS-only", "mqtt.itercraft.com → EC2 IP")
  }

  Rel(homeowner, esp32, "Configure", "WiFi")
  Rel(esp32, sensors, "Lit", "GPIO")
  Rel(esp32, cf_dns, "Résout", "DNS")
  Rel(esp32, mosquitto, "Publie", "MQTTS 8883")
  Rel(mosquitto, api2, "Forward", "Subscribe sensors/#")
  Rel(api2, db2, "Stocke", "JDBC")
  Rel(dashboard, api2, "GET /api/sensors/data", "HTTPS")
  Rel(api2, dashboard, "SSE sensor-data-change", "EventSource")
  Rel(homeowner, dashboard, "Consulte", "HTTPS")
`;

const chatOpsDiagram = `
C4Dynamic
  title Itercraft — ChatOps Infrastructure (C4 Dynamic)

  Person(devops, "DevOps", "Opérateur infrastructure")

  System_Boundary(slack_boundary, "Slack") {
    Container(slack_app, "Slack App", "Slash Command", "/infra apply ec2")
  }

  System_Boundary(aws_boundary, "AWS") {
    Container(apigw, "API Gateway", "HTTP API", "Endpoint webhook Slack")
    Container(lambda, "Lambda", "Node.js 20", "Vérifie signature, déclenche workflow")
    ContainerDb(dynamodb, "DynamoDB", "NoSQL", "Terraform state locking")
    ContainerDb(s3, "S3", "Object Storage", "Terraform remote state")
    Container(ec2, "EC2", "t3.large", "Instance cible du déploiement")
  }

  System_Boundary(github_boundary, "GitHub") {
    Container(actions, "GitHub Actions", "CI/CD", "Workflow terraform.yml")
    Container(oidc, "OIDC Provider", "Auth", "AssumeRoleWithWebIdentity")
  }

  Rel(devops, slack_app, "1. /infra apply ec2", "Slack")
  Rel(slack_app, apigw, "2. POST webhook", "HTTPS + signature")
  Rel(apigw, lambda, "3. Invoke", "AWS_PROXY")
  Rel(lambda, actions, "4. workflow_dispatch", "GitHub API")
  Rel(actions, oidc, "5. Get credentials", "OIDC token")
  Rel(oidc, s3, "6. Read/Write state", "IAM Role")
  Rel(oidc, dynamodb, "7. Lock state", "IAM Role")
  Rel(actions, ec2, "8. Terraform apply", "AWS API")
  Rel(actions, slack_app, "9. Notification", "Webhook")
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
    Container(loki, "Loki", "Log Aggregation", "Stockage et requêtes des logs (LogQL)")
    Container(promtail, "Promtail", "Log Collector", "Collecte des logs Docker containers")
    Container(tempo, "Tempo", "Distributed Tracing", "Stockage et requêtes des traces (OTLP)")
    Container(grafana, "Grafana", "Tableaux de bord", "Métriques, logs et traces unifiés")
  }

  System_Ext(claude2, "Claude API", "API Anthropic (suggestions activités)")
  System_Ext(meteofrance2, "Météo France", "API AROME PI (cartes WMS)")

  Rel(user, traefik, "HTTPS", "443")
  Rel(traefik, front, "Proxy", "/")
  Rel(traefik, api, "Proxy", "/api")
  Rel(traefik, kc, "Proxy", "/auth")
  Rel(traefik, grafana, "Proxy", "/grafana")
  Rel(front, api, "API REST + SSE", "HTTPS /api")
  Rel(front, kc, "Auth OIDC", "HTTPS /auth")
  Rel(api, db, "JDBC", "5432")
  Rel(api, kc, "JWT validation (JWK)", "HTTP")
  Rel(prom, api, "Scrape métriques", "/actuator/prometheus")
  Rel(promtail, api, "Collecte logs", "Docker socket")
  Rel(promtail, loki, "Push logs", "HTTP 3100")
  Rel(api, tempo, "Send traces", "OTLP 4317")
  Rel(grafana, prom, "Requêtes", "PromQL")
  Rel(grafana, loki, "Requêtes", "LogQL")
  Rel(grafana, tempo, "Requêtes", "TraceQL")
  Rel(api, claude2, "Suggestions activités", "HTTPS")
  Rel(api, meteofrance2, "Cartes WMS", "HTTPS")
`;

const observabilityDiagram = `
C4Container
  title Itercraft — Observability Stack (C4 Level 2)

  System_Boundary(app, "Application") {
    Container(api, "Backend API", "Spring Boot", "Génère métriques, logs et traces")
    Container(front, "Frontend", "React", "Génère logs browser")
    Container(kc, "Keycloak", "Auth Server", "Génère logs auth")
  }

  System_Boundary(observability, "Observability Stack") {
    Container(prom, "Prometheus", ":9090", "Stockage métriques time-series")
    Container(loki, "Loki", ":3100", "Stockage logs compressés")
    Container(tempo, "Tempo", ":3200", "Stockage traces distribuées")
    Container(promtail, "Promtail", "Agent", "Collecteur de logs Docker")
    Container(grafana, "Grafana", ":3001", "Dashboard unifié (3 piliers)")
  }

  Rel(api, prom, "Métriques", "/actuator/prometheus")
  Rel(api, tempo, "Traces", "OTLP gRPC :4317")
  Rel(promtail, api, "Scrape logs", "Docker socket")
  Rel(promtail, kc, "Scrape logs", "Docker socket")
  Rel(promtail, loki, "Push", "HTTP POST")
  Rel(grafana, prom, "Query", "PromQL")
  Rel(grafana, loki, "Query", "LogQL")
  Rel(grafana, tempo, "Query", "TraceQL")
  Rel(loki, tempo, "Trace ID link", "Derived fields")
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

      <section className="architecture-section">
        <h2>ChatOps Infrastructure (C4 Dynamic)</h2>
        <pre className="mermaid-diagram">{chatOpsDiagram}</pre>
      </section>

      <section className="architecture-section">
        <h2>IoT Architecture (MQTT)</h2>
        <pre className="mermaid-diagram">{iotDiagram}</pre>
      </section>

      <section className="architecture-section">
        <h2>Observability Stack (Logs, Traces, Metrics)</h2>
        <pre className="mermaid-diagram">{observabilityDiagram}</pre>
      </section>
    </div>
  );
}

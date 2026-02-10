import { useEffect, useRef } from 'react';
import mermaid from 'mermaid';
import '../public-page.css';

const connectionDiagram = `
sequenceDiagram
  participant Browser as Navigateur (React)
  participant Traefik as Traefik (Reverse Proxy)
  participant API as Backend (Spring Boot)
  participant SSE as SseService

  Browser->>Traefik: GET /api/events (Accept: text/event-stream)
  Traefik->>API: Proxy → GET /api/events
  API->>SSE: register()
  SSE-->>SSE: Crée SseEmitter (timeout ∞)
  SSE-->>SSE: Ajoute emitter à la liste
  SSE-->>API: SseEmitter
  API-->>Traefik: 200 OK (text/event-stream)
  Traefik-->>Browser: Connexion SSE ouverte

  Note over Browser,SSE: La connexion HTTP reste ouverte.<br/>Le navigateur écoute l'événement "subscription-change".
`;

const subscribeDiagram = `
sequenceDiagram
  participant UserA as Utilisateur A (Navigateur)
  participant UserB as Utilisateur B (Navigateur)
  participant API as Backend API
  participant Service as SubscriptionServiceImpl
  participant DB as PostgreSQL
  participant SSE as SseService

  UserA->>API: POST /api/subscriptions/tondeuse<br/>(Authorization: Bearer token)
  API->>Service: subscribe(keycloakSub, "tondeuse")
  Service->>DB: findOrCreateUser(sub)
  DB-->>Service: AppUser
  Service->>DB: findService("tondeuse")
  DB-->>Service: ServiceEntity
  Service->>DB: save(new Subscription)
  DB-->>Service: OK
  Service->>SSE: broadcast("subscription-change")

  par Diffusion à tous les clients connectés
    SSE-->>UserA: event: subscription-change<br/>data: refresh
    SSE-->>UserB: event: subscription-change<br/>data: refresh
  end

  API-->>UserA: 201 Created

  par Rafraîchissement automatique
    UserA->>API: GET /api/subscriptions
    API-->>UserA: [liste mise à jour]
    UserB->>API: GET /api/subscriptions
    API-->>UserB: [liste mise à jour]
  end
`;

const usageDiagram = `
sequenceDiagram
  participant User as Utilisateur (Navigateur)
  participant API as Backend API
  participant Service as SubscriptionServiceImpl
  participant DB as PostgreSQL
  participant SSE as SseService

  User->>API: POST /api/subscriptions/tondeuse/usages<br/>(Authorization + X-XSRF-TOKEN)
  API->>Service: addUsage(keycloakSub, "tondeuse")
  Service->>DB: findSubscription(sub, "tondeuse")
  DB-->>Service: Subscription
  Service->>DB: save(new ServiceUsage)
  DB-->>Service: OK
  Service->>SSE: broadcast("subscription-change")
  SSE-->>User: event: subscription-change<br/>data: refresh
  API-->>User: 201 Created

  User->>API: GET /api/subscriptions/tondeuse/usages
  API-->>User: [historique d'usage mis à jour]
`;

const disconnectDiagram = `
sequenceDiagram
  participant Browser as Navigateur (React)
  participant SSE as SseService

  Note over Browser,SSE: L'utilisateur quitte le Dashboard<br/>(changement de page ou fermeture)

  Browser->>Browser: useEffect cleanup → es.close()
  Browser->>SSE: Fermeture connexion HTTP

  SSE-->>SSE: onCompletion callback
  SSE-->>SSE: Retire emitter de la liste

  Note over SSE: L'emitter est nettoyé.<br/>Aucune fuite mémoire.
`;

mermaid.initialize({ startOnLoad: false, theme: 'default' });

export function SseDiagramsPage() {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current) {
      mermaid.run({ nodes: containerRef.current.querySelectorAll('.mermaid-diagram') });
    }
  }, []);

  return (
    <div className="public-page" ref={containerRef}>
      <h1>Server-Sent Events (SSE)</h1>

      <section className="intro" aria-label="Introduction">
        <p>
          Diagrammes de séquence UML décrivant le fonctionnement du temps réel dans Itercraft.
        </p>
      </section>

      <section aria-label="Connexion SSE">
        <h2>1. Connexion SSE</h2>
        <p>Le navigateur ouvre une connexion persistante vers <code>/api/events</code> (endpoint public, sans authentification).</p>
        <pre className="mermaid-diagram">{connectionDiagram}</pre>
      </section>

      <section aria-label="Abonnement à un service">
        <h2>2. Abonnement à un service</h2>
        <p>Lorsqu'un utilisateur s'abonne, le backend notifie tous les clients connectés via SSE.</p>
        <pre className="mermaid-diagram">{subscribeDiagram}</pre>
      </section>

      <section aria-label="Ajout d'un usage">
        <h2>3. Ajout d'un usage</h2>
        <p>L'ajout d'un usage déclenche le même mécanisme de broadcast SSE, incluant la protection CSRF.</p>
        <pre className="mermaid-diagram">{usageDiagram}</pre>
      </section>

      <section aria-label="Déconnexion et nettoyage">
        <h2>4. Déconnexion et nettoyage</h2>
        <p>Quand le composant React se démonte, la connexion SSE est fermée proprement.</p>
        <pre className="mermaid-diagram">{disconnectDiagram}</pre>
      </section>
    </div>
  );
}

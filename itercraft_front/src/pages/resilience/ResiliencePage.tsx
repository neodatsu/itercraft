import { useEffect, useRef, useState, useCallback } from 'react';
import mermaid from 'mermaid';
import { useAuth } from '../../auth/AuthProvider';
import './ResiliencePage.css';

interface CircuitBreakerStatus {
  name: string;
  state: string;
  failureRate: number;
  slowCallRate: number;
  bufferedCalls: number;
  failedCalls: number;
  slowCalls: number;
  notPermittedCalls: number;
}

interface ResilienceStatus {
  circuitBreakers: CircuitBreakerStatus[];
  timestamp: string;
}

const circuitBreakerStateDiagram = `
stateDiagram-v2
    [*] --> CLOSED

    CLOSED --> OPEN : Failure rate > threshold
    CLOSED --> CLOSED : Success / Failure below threshold

    OPEN --> HALF_OPEN : Wait duration elapsed
    OPEN --> OPEN : Requests blocked

    HALF_OPEN --> CLOSED : Success rate OK
    HALF_OPEN --> OPEN : Failure rate > threshold

    note right of CLOSED
        Normal operation
        All requests pass through
        Failures are counted
    end note

    note right of OPEN
        Circuit is "tripped"
        Requests fail fast
        Fallback is used
    end note

    note right of HALF_OPEN
        Testing recovery
        Limited requests allowed
        Determines next state
    end note
`;

const resiliencePatternsDiagram = `
flowchart TB
    subgraph Client
        REQ[Request]
    end

    subgraph "Resilience4j Patterns"
        CB[Circuit Breaker<br/>Fail fast on cascading failures]
        RETRY[Retry<br/>Automatic retry with backoff]
        TL[Time Limiter<br/>Timeout protection]
    end

    subgraph "External Services"
        MF[Météo France API]
        CL[Claude API]
    end

    subgraph "Fallback"
        FB[Fallback Response<br/>Graceful degradation]
    end

    REQ --> CB
    CB -->|CLOSED| RETRY
    CB -->|OPEN| FB
    RETRY -->|Success| MF
    RETRY -->|Success| CL
    RETRY -->|Max attempts| FB
    TL -->|Timeout| FB

    MF -->|Response| REQ
    CL -->|Response| REQ
    FB -->|Degraded response| REQ
`;

const configurationDiagram = `
flowchart LR
    subgraph "Météo France Circuit Breaker"
        MF_WINDOW[Sliding Window: 10 calls]
        MF_THRESHOLD[Failure Threshold: 50%]
        MF_WAIT[Open Duration: 30s]
        MF_RETRY[Retry: 3 attempts<br/>Exponential backoff]
        MF_TIMEOUT[Timeout: 10s]
    end

    subgraph "Claude Circuit Breaker"
        CL_WINDOW[Sliding Window: 5 calls]
        CL_THRESHOLD[Failure Threshold: 50%]
        CL_WAIT[Open Duration: 60s]
        CL_TIMEOUT[Timeout: 60s]
    end
`;

mermaid.initialize({ startOnLoad: false, theme: 'default' });

export function ResiliencePage() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { keycloak } = useAuth();
  const [status, setStatus] = useState<ResilienceStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = useCallback(async () => {
    try {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
      const response = await fetch(`${apiUrl}/api/resilience/status`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data = await response.json();
      setStatus(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (containerRef.current) {
      mermaid.run({ nodes: containerRef.current.querySelectorAll('.mermaid-diagram') });
    }
  }, []);

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, [fetchStatus]);

  const getStateClass = (state: string) => {
    switch (state) {
      case 'CLOSED': return 'state-closed';
      case 'OPEN': return 'state-open';
      case 'HALF_OPEN': return 'state-half-open';
      default: return '';
    }
  };

  const getStateEmoji = (state: string) => {
    switch (state) {
      case 'CLOSED': return '';
      case 'OPEN': return '';
      case 'HALF_OPEN': return '';
      default: return '';
    }
  };

  return (
    <div className="resilience-container" ref={containerRef}>
      <h1>Resilience Patterns</h1>

      <section className="resilience-section intro">
        <h2>Introduction</h2>
        <p>
          Les systèmes distribués modernes dépendent de nombreux services externes (APIs, bases de données, etc.).
          Lorsqu'un service devient lent ou indisponible, cela peut provoquer des <strong>cascading failures</strong>
          qui impactent l'ensemble de l'application.
        </p>
        <p>
          <strong>Resilience4j</strong> est une bibliothèque de tolérance aux pannes qui implémente plusieurs patterns
          pour protéger votre application :
        </p>
        <ul>
          <li><strong>Circuit Breaker</strong> : Coupe-circuit qui évite d'appeler un service défaillant</li>
          <li><strong>Retry</strong> : Réessaie automatiquement les appels échoués avec backoff exponentiel</li>
          <li><strong>Time Limiter</strong> : Limite le temps d'attente maximum pour un appel</li>
        </ul>
      </section>

      <section className="resilience-section">
        <h2>Circuit Breaker - États</h2>
        <p>
          Le Circuit Breaker fonctionne comme un disjoncteur électrique. Il surveille les appels vers un service
          et "se déclenche" (OPEN) lorsque trop d'erreurs surviennent.
        </p>
        <pre className="mermaid-diagram">{circuitBreakerStateDiagram}</pre>
        <div className="state-legend">
          <div className="legend-item">
            <span className="state-badge state-closed">CLOSED</span>
            <span>Fonctionnement normal, les requêtes passent</span>
          </div>
          <div className="legend-item">
            <span className="state-badge state-open">OPEN</span>
            <span>Circuit ouvert, les requêtes échouent immédiatement (fail fast)</span>
          </div>
          <div className="legend-item">
            <span className="state-badge state-half-open">HALF_OPEN</span>
            <span>Test de récupération, quelques requêtes autorisées</span>
          </div>
        </div>
      </section>

      <section className="resilience-section">
        <h2>Architecture des Patterns</h2>
        <p>
          Voici comment les différents patterns de résilience s'enchaînent dans Itercraft pour protéger
          les appels vers les APIs externes (Météo France et Claude).
        </p>
        <pre className="mermaid-diagram">{resiliencePatternsDiagram}</pre>
      </section>

      <section className="resilience-section">
        <h2>Configuration</h2>
        <p>
          Chaque service externe a sa propre configuration de circuit breaker, adaptée à ses caractéristiques.
        </p>
        <pre className="mermaid-diagram">{configurationDiagram}</pre>

        <table className="config-table">
          <thead>
            <tr>
              <th>Paramètre</th>
              <th>Météo France</th>
              <th>Claude</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Sliding Window</td>
              <td>10 appels</td>
              <td>5 appels</td>
              <td>Nombre d'appels analysés pour calculer le taux d'échec</td>
            </tr>
            <tr>
              <td>Failure Threshold</td>
              <td>50%</td>
              <td>50%</td>
              <td>Seuil d'échec pour déclencher l'ouverture du circuit</td>
            </tr>
            <tr>
              <td>Wait Duration</td>
              <td>30s</td>
              <td>60s</td>
              <td>Temps d'attente avant de tester la récupération</td>
            </tr>
            <tr>
              <td>Timeout</td>
              <td>10s</td>
              <td>60s</td>
              <td>Temps maximum pour un appel</td>
            </tr>
            <tr>
              <td>Retry</td>
              <td>3 tentatives</td>
              <td>-</td>
              <td>Réessais avec backoff exponentiel (x2)</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section className="resilience-section status-section">
        <h2>État en temps réel</h2>
        <p>
          Statut actuel des circuit breakers de l'application. Les données sont rafraîchies toutes les 5 secondes.
        </p>

        {loading && <div className="loading">Chargement...</div>}

        {error && (
          <div className="error-message">
            Impossible de charger le statut : {error}
          </div>
        )}

        {status && (
          <>
            <div className="status-grid">
              {status.circuitBreakers.map((cb) => (
                <div key={cb.name} className={`circuit-breaker-card ${getStateClass(cb.state)}`}>
                  <div className="cb-header">
                    <span className="cb-name">{cb.name}</span>
                    <span className={`cb-state ${getStateClass(cb.state)}`}>
                      {getStateEmoji(cb.state)} {cb.state}
                    </span>
                  </div>
                  <div className="cb-metrics">
                    <div className="metric">
                      <span className="metric-label">Failure Rate</span>
                      <span className="metric-value">{cb.failureRate.toFixed(1)}%</span>
                    </div>
                    <div className="metric">
                      <span className="metric-label">Buffered Calls</span>
                      <span className="metric-value">{cb.bufferedCalls}</span>
                    </div>
                    <div className="metric">
                      <span className="metric-label">Failed Calls</span>
                      <span className="metric-value">{cb.failedCalls}</span>
                    </div>
                    <div className="metric">
                      <span className="metric-label">Slow Calls</span>
                      <span className="metric-value">{cb.slowCalls}</span>
                    </div>
                    <div className="metric">
                      <span className="metric-label">Not Permitted</span>
                      <span className="metric-value">{cb.notPermittedCalls}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <div className="status-timestamp">
              Dernière mise à jour : {new Date(status.timestamp).toLocaleString('fr-FR')}
            </div>
          </>
        )}
      </section>

      <section className="resilience-section">
        <h2>Code Example</h2>
        <p>
          Voici comment le circuit breaker est implémenté dans le service Météo France :
        </p>
        <pre className="code-block">
{`@Override
@CircuitBreaker(name = "meteoFrance", fallbackMethod = "getMapImageFallback")
@Retry(name = "meteoFrance")
public byte[] getMapImage(String layer, double lat, double lon,
                          int width, int height) {
    // Appel vers l'API Météo France
    return restTemplate.getForObject(url, byte[].class);
}

// Méthode de fallback appelée quand le circuit est OPEN
private byte[] getMapImageFallback(String layer, double lat, double lon,
                                   int width, int height, Exception e) {
    log.warn("Météo France API unavailable: {}", e.getMessage());
    throw new MeteoServiceUnavailableException(
        "Service météo temporairement indisponible", e);
}`}
        </pre>
      </section>

      <section className="resilience-section">
        <h2>API Endpoints</h2>
        <table className="api-table">
          <thead>
            <tr>
              <th>Endpoint</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>GET /api/resilience/status</code></td>
              <td>Métriques détaillées des circuit breakers</td>
            </tr>
            <tr>
              <td><code>GET /api/resilience/health</code></td>
              <td>État de santé global (UP, DEGRADED, RECOVERING)</td>
            </tr>
            <tr>
              <td><code>GET /actuator/circuitbreakers</code></td>
              <td>Endpoint Actuator natif</td>
            </tr>
            <tr>
              <td><code>GET /actuator/circuitbreakerevents</code></td>
              <td>Historique des événements</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}

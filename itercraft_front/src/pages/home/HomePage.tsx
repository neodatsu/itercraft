import './HomePage.css';

export function HomePage() {
  return (
    <div className="home-container">
      <section className="home-hero">
        <h1 className="home-title">
          Bienvenue sur <span className="home-brand">Itercraft</span>
        </h1>
        <p className="home-subtitle">
          Une plateforme cloud-native expérimentale, construite selon les principes du software craftsmanship.
        </p>
      </section>

      <section className="home-section">
        <h2>Philosophie</h2>
        <p>
          Itercraft est un projet bac à sable explorant l'ingénierie logicielle moderne.
          Chaque couche est conçue pour être <strong>reproductible</strong>, <strong>testable</strong>
          {' '}et <strong>déployable depuis le code</strong>. Aucune étape manuelle, aucun raccourci.
        </p>
      </section>

      <section className="home-section">
        <h2>Principes</h2>
        <div className="home-cards">
          <div className="home-card">
            <h3>Software Craftsmanship</h3>
            <p>Clean code, Domain-Driven Design, tests unitaires et d'intégration, analyse de couverture.</p>
          </div>
          <div className="home-card">
            <h3>Infrastructure as Code</h3>
            <p>Stacks Terraform pour AWS (EC2, ECR, Budgets) et Cloudflare (DNS, SSL). Tout versionné, rien de manuel.</p>
          </div>
          <div className="home-card">
            <h3>Pipeline CI/CD</h3>
            <p>GitHub Actions pour le build, les tests, OWASP, SonarCloud et Lighthouse CI sur chaque commit.</p>
          </div>
          <div className="home-card">
            <h3>Sécurité by Design</h3>
            <p>OAuth2/OIDC avec PKCE via Keycloak, scan de vulnérabilités, CSRF, Cloudflare SSL, Traefik reverse proxy.</p>
          </div>
        </div>
      </section>

      <section className="home-section">
        <h2>Stack technique</h2>
        <div className="home-stack">
          <div className="home-stack-item">
            <span className="home-stack-icon">☕</span>
            <div>
              <strong>Backend</strong>
              <span>Java 25 &middot; Spring Boot 4 &middot; DDD</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">⚛️</span>
            <div>
              <strong>Frontend</strong>
              <span>React 19 &middot; TypeScript &middot; Vite</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">🔑</span>
            <div>
              <strong>Auth</strong>
              <span>Keycloak 26 &middot; OAuth2/OIDC &middot; PKCE</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">☁️</span>
            <div>
              <strong>Cloud</strong>
              <span>AWS &middot; Cloudflare &middot; Terraform &middot; Docker &middot; Traefik</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">🔄</span>
            <div>
              <strong>CI/CD</strong>
              <span>GitHub Actions &middot; JaCoCo &middot; SonarCloud &middot; Lighthouse</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">📊</span>
            <div>
              <strong>Monitoring</strong>
              <span>Prometheus &middot; Grafana &middot; Google Analytics</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">🛡️</span>
            <div>
              <strong>Sécurité</strong>
              <span>OWASP Dependency-Check &middot; SonarCloud &middot; CSRF</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">🤖</span>
            <div>
              <strong>IA / Vision</strong>
              <span>Ollama &middot; LLaVA &middot; Analyse d'images météo</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">⚡</span>
            <div>
              <strong>Temps réel</strong>
              <span>Server-Sent Events (SSE)</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

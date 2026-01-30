import './HomePage.css';

export function HomePage() {
  return (
    <div className="home-container">
      <section className="home-hero">
        <h1 className="home-title">
          Welcome to <span className="home-brand">Itercraft</span>
        </h1>
        <p className="home-subtitle">
          An experimental cloud-native platform built with craftsmanship principles.
        </p>
      </section>

      <section className="home-section">
        <h2>🎯 Philosophy</h2>
        <p>
          Itercraft is a sandbox project exploring modern software engineering at its best.
          Every layer is designed to be <strong>reproducible</strong>, <strong>testable</strong>,
          and <strong>deployable from code</strong>. No manual steps, no shortcuts.
        </p>
      </section>

      <section className="home-section">
        <h2>🏗️ Principles</h2>
        <div className="home-cards">
          <div className="home-card">
            <h3>✨ Software Craftsmanship</h3>
            <p>Clean code, Domain-Driven Design, unit &amp; integration tests, code coverage analysis.</p>
          </div>
          <div className="home-card">
            <h3>🔧 Infrastructure as Code</h3>
            <p>Terraform stacks for AWS (Route 53, ACM, ECR, Budgets). Everything versioned, nothing manual.</p>
          </div>
          <div className="home-card">
            <h3>🚀 CI/CD Pipeline</h3>
            <p>GitHub Actions for build, test, OWASP dependency check, SonarCloud quality gate on every commit.</p>
          </div>
          <div className="home-card">
            <h3>🔒 Security by Design</h3>
            <p>OAuth2/OIDC with PKCE via Keycloak, vulnerability scanning, SSL certificates managed as code.</p>
          </div>
        </div>
      </section>

      <section className="home-section">
        <h2>⚡ Tech Stack</h2>
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
              <span>AWS &middot; Terraform &middot; Docker &middot; Nginx</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">🔄</span>
            <div>
              <strong>CI/CD</strong>
              <span>GitHub Actions &middot; JaCoCo &middot; SonarCloud</span>
            </div>
          </div>
          <div className="home-stack-item">
            <span className="home-stack-icon">🛡️</span>
            <div>
              <strong>Security</strong>
              <span>OWASP Dependency-Check &middot; SonarCloud</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

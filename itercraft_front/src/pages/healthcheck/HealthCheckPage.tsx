import './HealthCheckPage.css';

export function HealthCheckPage() {
  return (
    <div className="healthcheck-container">
      <div className="healthcheck-status healthcheck-up">
        <h1>État de santé</h1>
        <p className="status">UP</p>
      </div>
    </div>
  );
}

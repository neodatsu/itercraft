import { useHealthCheck } from '../../hooks/useHealthCheck';
import './HealthCheckPage.css';

export function HealthCheckPage() {
  const { health, loading, error } = useHealthCheck();

  if (loading) {
    return <div className="healthcheck-container">Loading...</div>;
  }

  if (error) {
    return (
      <div className="healthcheck-container">
        <div className="healthcheck-status healthcheck-down">
          <h1>Health Check</h1>
          <p className="status">DOWN</p>
          <p className="error">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="healthcheck-container">
      <div className="healthcheck-status healthcheck-up">
        <h1>Health Check</h1>
        <p className="status">{health?.status}</p>
      </div>
    </div>
  );
}

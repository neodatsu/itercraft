import { useEffect, useState } from 'react';
import type { HealthStatus } from '../types/health';
import { fetchHealthStatus } from '../services/healthService';

interface UseHealthCheckResult {
  health: HealthStatus | null;
  loading: boolean;
  error: string | null;
}

export function useHealthCheck(): UseHealthCheckResult {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchHealthStatus()
      .then(setHealth)
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return { health, loading, error };
}

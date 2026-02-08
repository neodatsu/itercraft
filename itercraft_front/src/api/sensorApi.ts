const API_URL = import.meta.env.VITE_API_URL;

function authHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  };
}

export interface SensorDataPoint {
  measuredAt: string;
  deviceName: string;
  dhtTemperature: number | null;
  dhtHumidity: number | null;
  ntcTemperature: number | null;
  luminosity: number | null;
}

export async function getSensorData(
  accessToken: string,
  from?: string,
  to?: string
): Promise<SensorDataPoint[]> {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString() ? `?${params.toString()}` : '';

  const res = await fetch(`${API_URL}/api/sensors/data${query}`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec du chargement des données capteurs');
  return res.json();
}

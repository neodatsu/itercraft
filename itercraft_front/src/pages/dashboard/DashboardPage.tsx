import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { useAuth } from '../../auth/AuthProvider';
import { getSensorData, type SensorDataPoint } from '../../api/sensorApi';
import './DashboardPage.css';

function formatDateForInput(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function formatXAxis(value: string): string {
  const d = new Date(value);
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatTooltipLabel(label: ReactNode): ReactNode {
  if (typeof label === 'string') return formatXAxis(label);
  return label;
}

export function DashboardPage() {
  const { keycloak } = useAuth();
  const token = keycloak.token ?? '';
  const name = keycloak.tokenParsed?.preferred_username
    ?? keycloak.tokenParsed?.name
    ?? 'Utilisateur';

  const today = useMemo(() => new Date(), []);
  const sevenDaysAgo = useMemo(() => {
    const d = new Date(today);
    d.setDate(d.getDate() - 7);
    return d;
  }, [today]);

  const [from, setFrom] = useState(formatDateForInput(sevenDaysAgo));
  const [to, setTo] = useState(formatDateForInput(today));
  const [data, setData] = useState<SensorDataPoint[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (!token) return;
    const fromDate = new Date(from + 'T00:00:00');
    const toDate = new Date(to + 'T23:59:59');
    if (Number.isNaN(fromDate.getTime()) || Number.isNaN(toDate.getTime())) return;
    const result = await getSensorData(token, fromDate.toISOString(), toDate.toISOString());
    setData(result);
  }, [token, from, to]);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  useEffect(() => {
    const apiUrl = import.meta.env.VITE_API_URL as string;
    const es = new EventSource(`${apiUrl}/api/events`);
    es.addEventListener('sensor-data-change', () => { refresh(); });
    return () => es.close();
  }, [refresh]);

  const chartData = useMemo(() =>
    data.map((d) => ({
      time: d.measuredAt,
      'Temp. DHT (°C)': d.dhtTemperature,
      'Temp. NTC (°C)': d.ntcTemperature,
      'Humidité (%)': d.dhtHumidity,
      'Luminosité (%)': d.luminosity,
    })),
  [data]);

  const { tempDomain, pctDomain } = useMemo(() => {
    if (data.length === 0) return { tempDomain: [0, 40] as [number, number], pctDomain: [0, 100] as [number, number] };

    const temps = data.flatMap((d) => [d.dhtTemperature, d.ntcTemperature].filter((v): v is number => v != null));
    const pcts = data.flatMap((d) => [d.dhtHumidity, d.luminosity].filter((v): v is number => v != null));

    const margin = (min: number, max: number) => Math.max(1, (max - min) * 0.1);

    const tempMin = Math.min(...temps);
    const tempMax = Math.max(...temps);
    const tempM = margin(tempMin, tempMax);

    const pctMin = Math.min(...pcts);
    const pctMax = Math.max(...pcts);
    const pctM = margin(pctMin, pctMax);

    return {
      tempDomain: [Math.floor(tempMin - tempM), Math.ceil(tempMax + tempM)] as [number, number],
      pctDomain: [Math.max(0, Math.floor(pctMin - pctM)), Math.min(100, Math.ceil(pctMax + pctM))] as [number, number],
    };
  }, [data]);

  if (loading) {
    return (
      <div className="dashboard-container">
        <p className="dashboard-loading">Chargement...</p>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <h1>Tableau de bord</h1>
      <p className="dashboard-welcome">Bienvenue, <strong>{name}</strong></p>

      <section aria-label="Données capteurs" className="dashboard-section">
        <h2>Données capteurs</h2>

        <div className="dashboard-filters">
          <label htmlFor="filter-from">Du</label>
          <input
            id="filter-from"
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
          <label htmlFor="filter-to">Au</label>
          <input
            id="filter-to"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </div>

        {data.length === 0 ? (
          <p className="dashboard-empty">Aucune donnée capteur pour cette période.</p>
        ) : (
          <div className="dashboard-chart">
            <ResponsiveContainer width="100%" height={400}>
              <LineChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a2b50" />
                <XAxis
                  dataKey="time"
                  tickFormatter={formatXAxis}
                  stroke="#8080a0"
                  fontSize={12}
                />
                <YAxis
                  yAxisId="temp"
                  domain={tempDomain}
                  stroke="#8080a0"
                  fontSize={12}
                  label={{ value: '°C', position: 'insideTopLeft', fill: '#8080a0' }}
                />
                <YAxis
                  yAxisId="pct"
                  orientation="right"
                  domain={pctDomain}
                  stroke="#8080a0"
                  fontSize={12}
                  label={{ value: '%', position: 'insideTopRight', fill: '#8080a0' }}
                />
                <Tooltip
                  contentStyle={{ background: '#1f2040', border: '1px solid #2a2b50', borderRadius: 8 }}
                  labelFormatter={formatTooltipLabel}
                />
                <Legend />
                <Line yAxisId="temp" type="monotone" dataKey="Temp. DHT (°C)" stroke="#ef4444" dot={false} connectNulls />
                <Line yAxisId="temp" type="monotone" dataKey="Temp. NTC (°C)" stroke="#f97316" dot={false} connectNulls />
                <Line yAxisId="pct" type="monotone" dataKey="Humidité (%)" stroke="#3b82f6" dot={false} connectNulls />
                <Line yAxisId="pct" type="monotone" dataKey="Luminosité (%)" stroke="#eab308" dot={false} connectNulls />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>
    </div>
  );
}

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../auth/AuthProvider';
import {
  getActivities,
  getTotals,
  getHistory,
  startActivity,
  stopActivity,
  deleteSession,
  createActivity,
  type MaintenanceActivity,
  type MaintenanceTotals,
  type MaintenanceSession,
} from '../../api/maintenanceApi';
import './EntretienPage.css';

const MAX_DURATION_MINUTES = 4 * 60;
const WARNING_THRESHOLD_MINUTES = 3.5 * 60;

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours > 0) {
    return `${hours}h${mins.toString().padStart(2, '0')}`;
  }
  return `${mins}min`;
}

function formatElapsedTime(startedAt: string): string {
  const start = new Date(startedAt).getTime();
  const now = Date.now();
  const elapsedMs = Math.max(0, now - start);
  const totalSeconds = Math.floor(elapsedMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

function getElapsedMinutes(startedAt: string): number {
  const start = new Date(startedAt).getTime();
  return (Date.now() - start) / 60000;
}

function ActivityCard({ activity, token, onRefresh }: Readonly<{
  activity: MaintenanceActivity;
  token: string;
  onRefresh: () => Promise<void>;
}>) {
  const [now, setNow] = useState(Date.now());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!activity.isActive) return;
    const interval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(interval);
  }, [activity.isActive, now]);

  const elapsedMinutes = activity.startedAt ? getElapsedMinutes(activity.startedAt) : 0;
  const isWarning = activity.isActive && elapsedMinutes >= WARNING_THRESHOLD_MINUTES;

  async function handleStart() {
    setLoading(true);
    try {
      await startActivity(token, activity.serviceCode);
      await onRefresh();
    } finally {
      setLoading(false);
    }
  }

  async function handleStop() {
    setLoading(true);
    try {
      await stopActivity(token, activity.serviceCode);
      await onRefresh();
    } finally {
      setLoading(false);
    }
  }

  return (
    <section
      className={`activity-card ${activity.isActive ? 'active' : ''} ${isWarning ? 'warning' : ''}`}
      aria-label={activity.serviceLabel}
    >
      <h3>{activity.serviceLabel}</h3>
      {activity.isActive && activity.startedAt ? (
        <>
          <div className={`activity-timer ${isWarning ? 'warning' : ''}`}>
            {formatElapsedTime(activity.startedAt)}
          </div>
          {isWarning && (
            <div className="activity-today" style={{ color: '#f59e0b' }}>
              Arrêt auto dans {formatDuration(MAX_DURATION_MINUTES - Math.floor(elapsedMinutes))}
            </div>
          )}
        </>
      ) : (
        <div className="activity-timer">--:--:--</div>
      )}
      <div className="activity-today">
        Aujourd'hui : {formatDuration(activity.totalMinutesToday + (activity.isActive ? Math.floor(elapsedMinutes) : 0))}
      </div>
      <div className="activity-actions">
        {activity.isActive ? (
          <button
            className="btn btn-stop"
            onClick={handleStop}
            disabled={loading}
          >
            Stop
          </button>
        ) : (
          <button
            className="btn btn-start"
            onClick={handleStart}
            disabled={loading}
          >
            Démarrer
          </button>
        )}
      </div>
    </section>
  );
}

function TotalsSection({ totals }: Readonly<{ totals: MaintenanceTotals }>) {
  return (
    <section className="totals-section" aria-label="Totaux">
      <h2>Temps total</h2>
      <table className="totals-table">
        <thead>
          <tr>
            <th>Activité</th>
            <th>Jour</th>
            <th>Semaine</th>
            <th>Mois</th>
            <th>Année</th>
          </tr>
        </thead>
        <tbody>
          {totals.byActivity.map((activity) => (
            <tr key={activity.serviceCode}>
              <td>{activity.serviceLabel}</td>
              <td className="totals-value">{formatDuration(activity.todayMinutes)}</td>
              <td className="totals-value">{formatDuration(activity.weekMinutes)}</td>
              <td className="totals-value">{formatDuration(activity.monthMinutes)}</td>
              <td className="totals-value">{formatDuration(activity.yearMinutes)}</td>
            </tr>
          ))}
          <tr className="totals-row-total">
            <td><strong>Total</strong></td>
            <td className="totals-value"><strong>{formatDuration(totals.todayMinutes)}</strong></td>
            <td className="totals-value"><strong>{formatDuration(totals.weekMinutes)}</strong></td>
            <td className="totals-value"><strong>{formatDuration(totals.monthMinutes)}</strong></td>
            <td className="totals-value"><strong>{formatDuration(totals.yearMinutes)}</strong></td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}

function HistoryItem({ activity, token, onRefresh }: Readonly<{
  activity: MaintenanceActivity;
  token: string;
  onRefresh: () => Promise<void>;
}>) {
  const [open, setOpen] = useState(false);
  const [sessions, setSessions] = useState<MaintenanceSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    getHistory(token, activity.serviceCode)
      .then(setSessions)
      .finally(() => setLoading(false));
  }, [open, token, activity.serviceCode, refreshKey]);

  async function handleDelete(sessionId: string) {
    await deleteSession(token, sessionId);
    setRefreshKey((k) => k + 1);
    await onRefresh();
  }

  return (
    <div className="history-item">
      <button
        className="history-header"
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
      >
        <span className="history-title">{activity.serviceLabel}</span>
        <span className="history-count">
          {sessions.length > 0 && `${sessions.length} session${sessions.length === 1 ? '' : 's'}`}
        </span>
        <span className={`history-toggle ${open ? 'open' : ''}`}>▼</span>
      </button>
      {open && (
        <div className="history-content">
          {loading && (
            <p className="history-empty">Chargement...</p>
          )}
          {!loading && sessions.length === 0 && (
            <p className="history-empty">Aucune session enregistrée</p>
          )}
          {!loading && sessions.length > 0 && (
            <table className="history-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Début</th>
                  <th>Fin</th>
                  <th>Durée</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((session) => (
                  <tr key={session.id}>
                    <td>{new Date(session.startedAt).toLocaleDateString()}</td>
                    <td>{new Date(session.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
                    <td>
                      {session.endedAt
                        ? new Date(session.endedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                        : 'En cours'}
                      {session.autoStopped && <span className="auto-stopped">(auto)</span>}
                    </td>
                    <td>{session.durationMinutes === null ? '-' : formatDuration(session.durationMinutes)}</td>
                    <td className="history-actions">
                      <button
                        className="btn btn-delete"
                        onClick={() => handleDelete(session.id)}
                      >
                        Supprimer
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}

function AddActivityForm({ token, onRefresh }: Readonly<{
  token: string;
  onRefresh: () => Promise<void>;
}>) {
  const [label, setLabel] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement, SubmitEvent>) {
    e.preventDefault();
    if (!label.trim()) return;
    setLoading(true);
    try {
      await createActivity(token, label.trim());
      setLabel('');
      await onRefresh();
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="add-activity-form" onSubmit={handleSubmit}>
      <input
        type="text"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        placeholder="Nouvelle activité (ex: Jardinage)"
        className="add-activity-input"
        disabled={loading}
      />
      <button type="submit" className="btn btn-add" disabled={loading || !label.trim()}>
        Ajouter
      </button>
    </form>
  );
}

export function EntretienPage() {
  const { keycloak } = useAuth();
  const token = keycloak.token ?? '';

  const [activities, setActivities] = useState<MaintenanceActivity[]>([]);
  const [totals, setTotals] = useState<MaintenanceTotals | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const [acts, tots] = await Promise.all([
      getActivities(token),
      getTotals(token),
    ]);
    setActivities(acts);
    setTotals(tots);
  }, [token]);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  useEffect(() => {
    const apiUrl = import.meta.env.VITE_API_URL as string;
    const es = new EventSource(`${apiUrl}/api/events`);
    es.addEventListener('maintenance-change', () => { refresh(); });
    return () => es.close();
  }, [refresh]);

  const sortedActivities = useMemo(() => {
    return [...activities].sort((a, b) => a.serviceLabel.localeCompare(b.serviceLabel));
  }, [activities]);

  if (loading) {
    return (
      <div className="entretien-container">
        <p className="entretien-loading">Chargement...</p>
      </div>
    );
  }

  return (
    <div className="entretien-container">
      <h1>Entretien</h1>

      <AddActivityForm token={token} onRefresh={refresh} />

      <div className="activities-grid">
        {sortedActivities.map((activity) => (
          <ActivityCard
            key={activity.serviceCode}
            activity={activity}
            token={token}
            onRefresh={refresh}
          />
        ))}
      </div>

      {totals && <TotalsSection totals={totals} />}

      <section className="history-section" aria-label="Historique">
        <h2>Historique</h2>
        <div className="history-accordion">
          {sortedActivities.map((activity) => (
            <HistoryItem
              key={activity.serviceCode}
              activity={activity}
              token={token}
              onRefresh={refresh}
            />
          ))}
        </div>
      </section>
    </div>
  );
}

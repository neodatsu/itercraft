import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../auth/AuthProvider';
import {
  getSubscriptions,
  getUsageHistory,
  addUsage,
  removeUsage,
  type UserSubscription,
  type UsageRecord,
} from '../../api/subscriptionApi';
import './DashboardPage.css';

function UsageTable({ serviceCode, token, refreshKey, onRefresh }: Readonly<{ serviceCode: string; token: string; refreshKey: number; onRefresh: () => Promise<void> }>) {
  const [usages, setUsages] = useState<UsageRecord[]>([]);
  const [yearFilter, setYearFilter] = useState<string>('all');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getUsageHistory(token, serviceCode)
      .then(setUsages)
      .finally(() => setLoading(false));
  }, [token, serviceCode, refreshKey]);

  const years = useMemo(() => {
    const set = new Set(usages.map(u => new Date(u.usedAt).getFullYear().toString()));
    return Array.from(set).sort((a, b) => b.localeCompare(a));
  }, [usages]);

  const filtered = useMemo(() => {
    if (yearFilter === 'all') return usages;
    return usages.filter(u => new Date(u.usedAt).getFullYear().toString() === yearFilter);
  }, [usages, yearFilter]);

  if (loading) return <p className="usage-loading">Chargement...</p>;
  if (usages.length === 0) return null;

  return (
    <div className="usage-detail">
      <div className="usage-filter">
        <label htmlFor={`year-${serviceCode}`}>Année :</label>
        <select
          id={`year-${serviceCode}`}
          value={yearFilter}
          onChange={e => setYearFilter(e.target.value)}
        >
          <option value="all">Toutes</option>
          {years.map(y => <option key={y} value={y}>{y}</option>)}
        </select>
        <span className="usage-count-label">{filtered.length} usage{filtered.length === 1 ? '' : 's'}</span>
      </div>
      <table className="dashboard-table usage-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Date</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {filtered.map((u, i) => (
            <tr key={u.id}>
              <td className="usage-index">{filtered.length - i}</td>
              <td>{new Date(u.usedAt).toLocaleString()}</td>
              <td className="usage-actions">
                <button
                  className="btn btn-sm btn-danger"
                  onClick={async () => {
                    await removeUsage(token, serviceCode, u.id);
                    await onRefresh();
                  }}
                >Supprimer</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function DashboardPage() {
  const { keycloak } = useAuth();
  const name = keycloak.tokenParsed?.preferred_username
    ?? keycloak.tokenParsed?.name
    ?? 'Utilisateur';

  const [subscriptions, setSubscriptions] = useState<UserSubscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  const token = keycloak.token ?? '';

  const refresh = useCallback(async () => {
    const subs = await getSubscriptions(token);
    setSubscriptions(subs);
    setRefreshKey(k => k + 1);
  }, [token]);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  useEffect(() => {
    const apiUrl = import.meta.env.VITE_API_URL as string;
    const es = new EventSource(`${apiUrl}/api/events`);
    es.addEventListener('subscription-change', () => { refresh(); });
    return () => es.close();
  }, [refresh]);

  async function handleAddUsage(code: string) {
    await addUsage(token, code);
    await refresh();
  }

  if (loading) return <div className="dashboard-container"><p>Chargement...</p></div>;

  return (
    <div className="dashboard-container">
      <h1>Tableau de bord</h1>
      <p className="dashboard-welcome">Bienvenue, <strong>{name}</strong></p>

      <div className="services-grid">
        {subscriptions.map(sub => (
          <section key={sub.serviceCode} className="dashboard-section">
            <div className="service-header">
              <h2>{sub.serviceLabel}</h2>
              <div className="dashboard-actions">
                <button className="btn btn-sm btn-primary" onClick={() => handleAddUsage(sub.serviceCode)}>+ Usage</button>
              </div>
            </div>
            <UsageTable serviceCode={sub.serviceCode} token={token} refreshKey={refreshKey} onRefresh={refresh} />
          </section>
        ))}
      </div>
    </div>
  );
}

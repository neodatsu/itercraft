import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthProvider';
import {
  getSubscriptions,
  getServices,
  subscribe,
  unsubscribe,
  addUsage,
  removeUsage,
  type UserSubscription,
  type ServiceInfo,
} from '../../api/subscriptionApi';
import './DashboardPage.css';

export function DashboardPage() {
  const { keycloak } = useAuth();
  const name = keycloak.tokenParsed?.preferred_username
    ?? keycloak.tokenParsed?.name
    ?? 'User';

  const [subscriptions, setSubscriptions] = useState<UserSubscription[]>([]);
  const [services, setServices] = useState<ServiceInfo[]>([]);
  const [selectedService, setSelectedService] = useState('');
  const [loading, setLoading] = useState(true);

  const token = keycloak.token ?? '';

  const refresh = useCallback(async () => {
    const [subs, svcs] = await Promise.all([
      getSubscriptions(token),
      getServices(token),
    ]);
    setSubscriptions(subs);
    setServices(svcs);
  }, [token]);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  const availableServices = services.filter(
    s => !subscriptions.some(sub => sub.serviceCode === s.code)
  );

  async function handleSubscribe() {
    if (!selectedService) return;
    await subscribe(token, selectedService);
    setSelectedService('');
    await refresh();
  }

  async function handleUnsubscribe(code: string) {
    await unsubscribe(token, code);
    await refresh();
  }

  async function handleAddUsage(code: string) {
    await addUsage(token, code);
    await refresh();
  }

  async function handleRemoveUsage(code: string) {
    await removeUsage(token, code);
    await refresh();
  }

  if (loading) return <div className="dashboard-container"><p>Loading...</p></div>;

  return (
    <div className="dashboard-container">
      <h1>Dashboard</h1>
      <p className="dashboard-welcome">Welcome, <strong>{name}</strong></p>

      <section className="dashboard-section">
        <h2>My Services</h2>
        {subscriptions.length === 0 ? (
          <p className="dashboard-empty">No subscriptions yet.</p>
        ) : (
          <table className="dashboard-table">
            <thead>
              <tr>
                <th>Service</th>
                <th>Usage</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {subscriptions.map(sub => (
                <tr key={sub.serviceCode}>
                  <td>{sub.serviceLabel}</td>
                  <td className="dashboard-count">{sub.usageCount}</td>
                  <td className="dashboard-actions">
                    <button className="btn btn-sm btn-primary" onClick={() => handleAddUsage(sub.serviceCode)}>+</button>
                    <button
                      className="btn btn-sm btn-secondary"
                      onClick={() => handleRemoveUsage(sub.serviceCode)}
                      disabled={sub.usageCount === 0}
                    >-</button>
                    <button className="btn btn-sm btn-danger" onClick={() => handleUnsubscribe(sub.serviceCode)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {availableServices.length > 0 && (
        <section className="dashboard-section">
          <h2>Add a Service</h2>
          <div className="dashboard-add">
            <select
              className="dashboard-select"
              value={selectedService}
              onChange={e => setSelectedService(e.target.value)}
            >
              <option value="">-- Select --</option>
              {availableServices.map(s => (
                <option key={s.code} value={s.code}>{s.label}</option>
              ))}
            </select>
            <button
              className="btn btn-primary"
              onClick={handleSubscribe}
              disabled={!selectedService}
            >Subscribe</button>
          </div>
        </section>
      )}
    </div>
  );
}

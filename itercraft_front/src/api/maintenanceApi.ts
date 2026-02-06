const API_URL = import.meta.env.VITE_API_URL;

function getCsrfToken(): string {
  const match = /XSRF-TOKEN=([^;]+)/.exec(document.cookie);
  return match ? decodeURIComponent(match[1]) : '';
}

function authHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  };
}

function mutationHeaders(accessToken: string): Record<string, string> {
  return {
    ...authHeaders(accessToken),
    'X-XSRF-TOKEN': getCsrfToken(),
  };
}

export interface MaintenanceActivity {
  serviceCode: string;
  serviceLabel: string;
  isActive: boolean;
  startedAt: string | null;
  totalMinutesToday: number;
}

export interface MaintenanceSession {
  id: string;
  serviceCode: string;
  serviceLabel: string;
  startedAt: string;
  endedAt: string | null;
  durationMinutes: number | null;
  autoStopped: boolean;
}

export interface ActivityTotals {
  serviceCode: string;
  serviceLabel: string;
  todayMinutes: number;
  weekMinutes: number;
  monthMinutes: number;
  yearMinutes: number;
}

export interface MaintenanceTotals {
  todayMinutes: number;
  weekMinutes: number;
  monthMinutes: number;
  yearMinutes: number;
  byActivity: ActivityTotals[];
}

export async function getActivities(accessToken: string): Promise<MaintenanceActivity[]> {
  const res = await fetch(`${API_URL}/api/maintenance/activities`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec du chargement des activités');
  return res.json();
}

export async function startActivity(accessToken: string, serviceCode: string): Promise<MaintenanceSession> {
  const res = await fetch(`${API_URL}/api/maintenance/activities/${serviceCode}/start`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec du démarrage de l\'activité');
  return res.json();
}

export async function stopActivity(accessToken: string, serviceCode: string): Promise<MaintenanceSession> {
  const res = await fetch(`${API_URL}/api/maintenance/activities/${serviceCode}/stop`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec de l\'arrêt de l\'activité');
  return res.json();
}

export async function getTotals(accessToken: string): Promise<MaintenanceTotals> {
  const res = await fetch(`${API_URL}/api/maintenance/totals`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec du chargement des totaux');
  return res.json();
}

export async function getHistory(accessToken: string, serviceCode: string): Promise<MaintenanceSession[]> {
  const res = await fetch(`${API_URL}/api/maintenance/activities/${serviceCode}/history`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec du chargement de l\'historique');
  return res.json();
}

export async function deleteSession(accessToken: string, sessionId: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/maintenance/sessions/${sessionId}`, {
    method: 'DELETE',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec de la suppression de la session');
}

export async function createActivity(accessToken: string, label: string): Promise<MaintenanceActivity> {
  const res = await fetch(`${API_URL}/api/maintenance/activities`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
    body: JSON.stringify({ label }),
  });
  if (!res.ok) throw new Error('Échec de la création de l\'activité');
  return res.json();
}

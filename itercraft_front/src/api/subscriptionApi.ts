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

export interface UserSubscription {
  serviceCode: string;
  serviceLabel: string;
  usageCount: number;
}

export interface UsageRecord {
  id: string;
  usedAt: string;
}

export interface ServiceInfo {
  code: string;
  label: string;
  description: string;
}

export async function getSubscriptions(accessToken: string): Promise<UserSubscription[]> {
  const res = await fetch(`${API_URL}/api/subscriptions`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to fetch subscriptions');
  return res.json();
}

export async function getServices(accessToken: string): Promise<ServiceInfo[]> {
  const res = await fetch(`${API_URL}/api/services`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to fetch services');
  return res.json();
}

export async function getUsageHistory(accessToken: string, serviceCode: string): Promise<UsageRecord[]> {
  const res = await fetch(`${API_URL}/api/subscriptions/${serviceCode}/usages`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to fetch usage history');
  return res.json();
}

export async function subscribe(accessToken: string, serviceCode: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/subscriptions/${serviceCode}`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to subscribe');
}

export async function unsubscribe(accessToken: string, serviceCode: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/subscriptions/${serviceCode}`, {
    method: 'DELETE',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to unsubscribe');
}

export async function addUsage(accessToken: string, serviceCode: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/subscriptions/${serviceCode}/usages`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to add usage');
}

export async function removeUsage(accessToken: string, serviceCode: string, usageId: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/subscriptions/${serviceCode}/usages/${usageId}`, {
    method: 'DELETE',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to remove usage');
}

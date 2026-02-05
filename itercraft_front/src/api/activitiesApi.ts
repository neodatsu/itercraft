const API_URL = import.meta.env.VITE_API_URL;

function getCsrfToken(): string {
  const match = /XSRF-TOKEN=([^;]+)/.exec(document.cookie);
  return match ? decodeURIComponent(match[1]) : '';
}

export interface Activity {
  name: string;
  description: string;
  icon: string;
}

export interface ActivitySuggestion {
  location: string;
  activities: {
    morning: Activity[];
    afternoon: Activity[];
    evening: Activity[];
  };
  summary: string;
}

export async function fetchActivitySuggestions(
  accessToken: string,
  lat: number,
  lon: number,
  location: string,
): Promise<ActivitySuggestion> {
  const params = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
    location,
  });
  const res = await fetch(`${API_URL}/api/activities/suggest?${params}`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'X-XSRF-TOKEN': getCsrfToken(),
    },
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Impossible de charger les suggestions d\'activités');
  return res.json();
}

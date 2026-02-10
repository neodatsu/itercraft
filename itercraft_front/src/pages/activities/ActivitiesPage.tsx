import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthProvider';
import { fetchActivitySuggestions } from '../../api/activitiesApi';
import type { ActivitySuggestion, Activity } from '../../api/activitiesApi';
import './ActivitiesPage.css';

const DEFAULT_LAT = 43.9283;
const DEFAULT_LON = 2.1480;

const ICON_MAP: Record<string, string> = {
  bike: 'https://cdn-icons-png.flaticon.com/32/2972/2972185.png',
  walk: 'https://cdn-icons-png.flaticon.com/32/2554/2554894.png',
  pool: 'https://cdn-icons-png.flaticon.com/32/2784/2784593.png',
  home: 'https://cdn-icons-png.flaticon.com/32/1946/1946488.png',
  run: 'https://cdn-icons-png.flaticon.com/32/384/384276.png',
  hike: 'https://cdn-icons-png.flaticon.com/32/2503/2503508.png',
  picnic: 'https://cdn-icons-png.flaticon.com/32/3142/3142024.png',
  garden: 'https://cdn-icons-png.flaticon.com/32/628/628324.png',
  read: 'https://cdn-icons-png.flaticon.com/32/2702/2702134.png',
  yoga: 'https://cdn-icons-png.flaticon.com/32/2647/2647625.png',
  cinema: 'https://cdn-icons-png.flaticon.com/32/3163/3163478.png',
};

async function reverseGeocode(lat: number, lon: number): Promise<string> {
  const res = await fetch(
    `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=fr`,
    { headers: { 'User-Agent': 'Itercraft/1.0' } },
  );
  if (!res.ok) return '';
  const data = await res.json();
  return data.display_name ?? '';
}

function getShortLocation(fullAddress: string): string {
  const parts = fullAddress.split(',');
  return parts.slice(0, 2).join(',').trim();
}

interface ActivityCardProps {
  activity: Activity;
}

function ActivityCard({ activity }: Readonly<ActivityCardProps>) {
  const iconUrl = ICON_MAP[activity.icon] || ICON_MAP.walk;
  return (
    <div className="activity-card">
      <img src={iconUrl} alt="" className="activity-icon" />
      <div className="activity-info">
        <h4>{activity.name}</h4>
        <p>{activity.description}</p>
      </div>
    </div>
  );
}

interface TimeSlotSectionProps {
  title: string;
  activities: Activity[];
}

function TimeSlotSection({ title, activities }: Readonly<TimeSlotSectionProps>) {
  if (activities.length === 0) return null;
  return (
    <section className="activities-timeslot" aria-label={title}>
      <h3>{title}</h3>
      <div className="activities-cards">
        {activities.map((activity, index) => (
          <ActivityCard key={`${activity.name}-${index}`} activity={activity} />
        ))}
      </div>
    </section>
  );
}

export function ActivitiesPage() {
  const { keycloak } = useAuth();
  const token = keycloak.token ?? '';

  const [lat, setLat] = useState(DEFAULT_LAT);
  const [lon, setLon] = useState(DEFAULT_LON);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [geoLoading, setGeoLoading] = useState(false);
  const [address, setAddress] = useState('');
  const [suggestion, setSuggestion] = useState<ActivitySuggestion | null>(null);

  const loadActivities = useCallback(async (actLat: number, actLon: number) => {
    setLoading(true);
    setError(null);
    setSuggestion(null);
    try {
      const addr = await reverseGeocode(actLat, actLon);
      setAddress(addr);
      const shortLocation = getShortLocation(addr) || `${actLat}, ${actLon}`;
      const result = await fetchActivitySuggestions(token, actLat, actLon, shortLocation);
      setSuggestion(result);
    } catch {
      setError('Impossible de charger les suggestions d\'activités.');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadActivities(lat, lon);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  function handleGeolocate() {
    if (!navigator.geolocation) {
      setError('La géolocalisation n\'est pas supportée par votre navigateur.');
      return;
    }
    setGeoLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const newLat = Math.round(pos.coords.latitude * 10000) / 10000;
        const newLon = Math.round(pos.coords.longitude * 10000) / 10000;
        setLat(newLat);
        setLon(newLon);
        setGeoLoading(false);
        loadActivities(newLat, newLon);
      },
      () => {
        setError('Impossible d\'obtenir votre position.');
        setGeoLoading(false);
      },
    );
  }

  function handleSubmit(e: React.SyntheticEvent<HTMLFormElement, SubmitEvent>) {
    e.preventDefault();
    loadActivities(lat, lon);
  }

  const osmUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${lon - 0.02},${lat - 0.015},${lon + 0.02},${lat + 0.015}&layer=mapnik&marker=${lat},${lon}`;

  return (
    <div className="activities-container">
      <h1>Activités</h1>

      <form className="activities-controls" onSubmit={handleSubmit}>
        <div className="activities-field">
          <label htmlFor="activities-lat">Latitude</label>
          <input
            id="activities-lat"
            type="number"
            step="0.0001"
            value={lat}
            onChange={e => setLat(Number(e.target.value))}
          />
        </div>
        <div className="activities-field">
          <label htmlFor="activities-lon">Longitude</label>
          <input
            id="activities-lon"
            type="number"
            step="0.0001"
            value={lon}
            onChange={e => setLon(Number(e.target.value))}
          />
        </div>
        <div className="activities-actions">
          <button className="btn btn-primary btn-sm" type="submit" disabled={loading}>
            {loading ? 'Chargement...' : 'Rechercher'}
          </button>
          <button
            className="btn btn-secondary btn-sm"
            type="button"
            onClick={handleGeolocate}
            disabled={geoLoading || loading}
          >
            {geoLoading ? 'Localisation...' : 'Me localiser'}
          </button>
        </div>
      </form>

      {address && (
        <p className="activities-address">{address}</p>
      )}

      {loading && <p className="activities-loading">Analyse météo et suggestions en cours...</p>}
      {error && <p className="activities-error">{error}</p>}

      {!loading && suggestion && (
        <>
          <div className="activities-summary">
            <p>{suggestion.summary}</p>
          </div>

          <div className="activities-content">
            <div className="activities-map">
              <h2>Localisation</h2>
              <iframe
                title="OpenStreetMap"
                src={osmUrl}
                width="100%"
                height="300"
                style={{ border: '1px solid #ddd', borderRadius: 8 }}
              />
            </div>

            <div className="activities-suggestions">
              <TimeSlotSection title="Matin" activities={suggestion.activities.morning} />
              <TimeSlotSection title="Après-midi" activities={suggestion.activities.afternoon} />
              <TimeSlotSection title="Soir" activities={suggestion.activities.evening} />
            </div>
          </div>
        </>
      )}
    </div>
  );
}

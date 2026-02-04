import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthProvider';
import { fetchMeteoAnalysis, fetchMeteoMap, LAYERS } from '../../api/meteoApi';
import './MeteoPage.css';

const DEFAULT_LAT = 48.8566;
const DEFAULT_LON = 2.3522;

async function reverseGeocode(lat: number, lon: number): Promise<string> {
  const res = await fetch(
    `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=fr`,
    { headers: { 'User-Agent': 'Itercraft/1.0' } },
  );
  if (!res.ok) return '';
  const data = await res.json();
  return data.display_name ?? '';
}

export function MeteoPage() {
  const { keycloak } = useAuth();
  const token = keycloak.token ?? '';

  const [lat, setLat] = useState(DEFAULT_LAT);
  const [lon, setLon] = useState(DEFAULT_LON);
  const [layer, setLayer] = useState(LAYERS[0].code);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [geoLoading, setGeoLoading] = useState(false);
  const [address, setAddress] = useState('');
  const [analysis, setAnalysis] = useState<string | null>(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);

  const loadMap = useCallback(async (mapLat: number, mapLon: number, mapLayer: string) => {
    setLoading(true);
    setError(null);
    setAnalysis(null);
    try {
      const [url, addr] = await Promise.all([
        fetchMeteoMap(token, mapLayer, mapLat, mapLon),
        reverseGeocode(mapLat, mapLon),
      ]);
      setImageUrl(prev => {
        if (prev) URL.revokeObjectURL(prev);
        return url;
      });
      setAddress(addr);
      setLoading(false);

      // Launch AI analysis in background (non-blocking)
      if (addr) {
        setAnalysisLoading(true);
        fetchMeteoAnalysis(token, mapLayer, mapLat, mapLon, addr)
          .then(setAnalysis)
          .catch(() => { /* graceful degradation */ })
          .finally(() => setAnalysisLoading(false));
      }
    } catch {
      setError('Impossible de charger la carte météo.');
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadMap(lat, lon, layer);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      if (imageUrl) URL.revokeObjectURL(imageUrl);
    };
  }, [imageUrl]);

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
        loadMap(newLat, newLon, layer);
      },
      () => {
        setError('Impossible d\'obtenir votre position.');
        setGeoLoading(false);
      },
    );
  }

  function handleSubmit(e: React.SyntheticEvent<HTMLFormElement, SubmitEvent>) {
    e.preventDefault();
    loadMap(lat, lon, layer);
  }

  const osmUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${lon - 0.02},${lat - 0.015},${lon + 0.02},${lat + 0.015}&layer=mapnik&marker=${lat},${lon}`;

  return (
    <div className="meteo-container">
      <h1>Météo</h1>

      <form className="meteo-controls" onSubmit={handleSubmit}>
        <div className="meteo-field">
          <label htmlFor="meteo-lat">Latitude</label>
          <input
            id="meteo-lat"
            type="number"
            step="0.0001"
            value={lat}
            onChange={e => setLat(Number(e.target.value))}
          />
        </div>
        <div className="meteo-field">
          <label htmlFor="meteo-lon">Longitude</label>
          <input
            id="meteo-lon"
            type="number"
            step="0.0001"
            value={lon}
            onChange={e => setLon(Number(e.target.value))}
          />
        </div>
        <div className="meteo-field">
          <label htmlFor="meteo-layer">Couche</label>
          <select
            id="meteo-layer"
            value={layer}
            onChange={e => setLayer(e.target.value)}
          >
            {LAYERS.map(l => (
              <option key={l.code} value={l.code}>{l.label}</option>
            ))}
          </select>
        </div>
        <div className="meteo-actions">
          <button className="btn btn-primary btn-sm" type="submit">Afficher</button>
          <button
            className="btn btn-secondary btn-sm"
            type="button"
            onClick={handleGeolocate}
            disabled={geoLoading}
          >
            {geoLoading ? 'Localisation...' : 'Me localiser'}
          </button>
        </div>
      </form>

      {address && (
        <p className="meteo-address">{address}</p>
      )}

      {loading && <p className="meteo-loading">Chargement de la carte...</p>}
      {error && <p className="meteo-error">{error}</p>}

      {!loading && (imageUrl || address) && (
        <div className="meteo-grid">
          <div className="meteo-map">
            <h2>Plan</h2>
            <iframe
              title="OpenStreetMap"
              src={osmUrl}
              width="100%"
              height="350"
              style={{ border: '1px solid #ddd', borderRadius: 8 }}
            />
          </div>
          {imageUrl && (
            <div className="meteo-map">
              <h2>{LAYERS.find(l => l.code === layer)?.label}</h2>
              <img src={imageUrl} alt={`Carte météo — ${LAYERS.find(l => l.code === layer)?.label}`} />
            </div>
          )}
        </div>
      )}

      {analysisLoading && (
        <p className="meteo-analysis-loading">Analyse IA en cours...</p>
      )}

      {analysis && (
        <div className="meteo-analysis">
          <h2>Analyse météorologique</h2>
          <p>{analysis}</p>
        </div>
      )}
    </div>
  );
}

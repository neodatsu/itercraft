const API_URL = import.meta.env.VITE_API_URL;

export interface MeteoLayer {
  code: string;
  label: string;
}

export const LAYERS: MeteoLayer[] = [
  { code: 'TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND', label: 'Température' },
  { code: 'TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE', label: 'Précipitations' },
  { code: 'TOTAL_PRECIPITATION_RATE__GROUND_OR_WATER_SURFACE', label: 'Intensité précipitations' },
  { code: 'WIND_SPEED_GUST__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND', label: 'Rafales de vent' },
  { code: 'RELATIVE_HUMIDITY__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND', label: 'Humidité' },
  { code: 'LOW_CLOUD_COVER__GROUND_OR_WATER_SURFACE', label: 'Nébulosité' },
];

export async function fetchMeteoMap(
  accessToken: string,
  layer: string,
  lat: number,
  lon: number,
): Promise<string> {
  const params = new URLSearchParams({
    layer,
    lat: String(lat),
    lon: String(lon),
    width: '512',
    height: '512',
  });
  const res = await fetch(`${API_URL}/api/meteo/map?${params}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Échec du chargement de la carte météo');
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}

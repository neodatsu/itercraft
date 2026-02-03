import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeteoPage } from './MeteoPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/meteoApi', () => ({
  LAYERS: [
    { code: 'TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND', label: 'Température' },
    { code: 'TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE', label: 'Précipitations' },
  ],
  fetchMeteoMap: vi.fn(),
  fetchMeteoAnalysis: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';
import { fetchMeteoMap, fetchMeteoAnalysis } from '../../api/meteoApi';

const mockUseAuth = vi.mocked(useAuth);
const mockFetchMeteoMap = vi.mocked(fetchMeteoMap);
const mockFetchMeteoAnalysis = vi.mocked(fetchMeteoAnalysis);

// Mock global fetch for reverseGeocode
const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

// Mock URL.createObjectURL and revokeObjectURL
const mockCreateObjectURL = vi.fn(() => 'blob:http://localhost/fake-image');
const mockRevokeObjectURL = vi.fn();
vi.stubGlobal('URL', {
  ...URL,
  createObjectURL: mockCreateObjectURL,
  revokeObjectURL: mockRevokeObjectURL,
});

describe('MeteoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    mockFetchMeteoMap.mockResolvedValue('blob:http://localhost/fake-image');
    mockFetchMeteoAnalysis.mockResolvedValue('Temps ensoleillé sur Paris.');
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ display_name: 'Paris, France' }),
    });
  });

  it('renders page title', () => {
    render(<MeteoPage />);
    expect(screen.getByText('Météo')).toBeInTheDocument();
  });

  it('renders latitude and longitude inputs', () => {
    render(<MeteoPage />);
    expect(screen.getByLabelText('Latitude')).toBeInTheDocument();
    expect(screen.getByLabelText('Longitude')).toBeInTheDocument();
  });

  it('renders layer selector with options', () => {
    render(<MeteoPage />);
    expect(screen.getByLabelText('Couche')).toBeInTheDocument();
    expect(screen.getByText('Température')).toBeInTheDocument();
    expect(screen.getByText('Précipitations')).toBeInTheDocument();
  });

  it('renders action buttons', () => {
    render(<MeteoPage />);
    expect(screen.getByText('Afficher')).toBeInTheDocument();
    expect(screen.getByText('Me localiser')).toBeInTheDocument();
  });

  it('loads map on initial render', async () => {
    render(<MeteoPage />);

    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledWith(
        'fake-token',
        'TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND',
        48.8566,
        2.3522
      );
    });
  });

  it('displays address after loading', async () => {
    render(<MeteoPage />);

    await waitFor(() => {
      expect(screen.getByText('Paris, France')).toBeInTheDocument();
    });
  });

  it('displays analysis after loading', async () => {
    render(<MeteoPage />);

    await waitFor(() => {
      expect(screen.getByText('Temps ensoleillé sur Paris.')).toBeInTheDocument();
    });
  });

  it('shows loading state', async () => {
    mockFetchMeteoMap.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<MeteoPage />);

    expect(screen.getByText('Chargement de la carte...')).toBeInTheDocument();
  });

  it('displays error when map loading fails', async () => {
    mockFetchMeteoMap.mockRejectedValue(new Error('Network error'));

    render(<MeteoPage />);

    await waitFor(() => {
      expect(screen.getByText('Impossible de charger la carte météo.')).toBeInTheDocument();
    });
  });

  it('allows changing latitude value', async () => {
    const user = userEvent.setup();
    render(<MeteoPage />);

    const latInput = screen.getByLabelText('Latitude');
    await user.clear(latInput);
    await user.type(latInput, '45.0');

    expect(latInput).toHaveValue(45);
  });

  it('allows changing longitude value', async () => {
    const user = userEvent.setup();
    render(<MeteoPage />);

    const lonInput = screen.getByLabelText('Longitude');
    await user.clear(lonInput);
    await user.type(lonInput, '3.0');

    expect(lonInput).toHaveValue(3);
  });

  it('allows changing layer', async () => {
    const user = userEvent.setup();
    render(<MeteoPage />);

    const layerSelect = screen.getByLabelText('Couche');
    await user.selectOptions(layerSelect, 'TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE');

    expect(layerSelect).toHaveValue('TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE');
  });

  it('submits form and reloads map', async () => {
    const user = userEvent.setup();
    render(<MeteoPage />);

    // Wait for initial load
    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(1);
    });

    const submitButton = screen.getByText('Afficher');
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(2);
    });
  });

  it('handles geolocation success', async () => {
    const mockGeolocation = {
      getCurrentPosition: vi.fn((success) => {
        success({
          coords: { latitude: 45.764, longitude: 4.8357 },
        });
      }),
    };
    Object.defineProperty(navigator, 'geolocation', {
      value: mockGeolocation,
      writable: true,
    });

    const user = userEvent.setup();
    render(<MeteoPage />);

    // Wait for initial load
    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(1);
    });

    const geoButton = screen.getByText('Me localiser');
    await user.click(geoButton);

    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledWith(
        'fake-token',
        'TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND',
        45.764,
        4.8357
      );
    });
  });

  it('handles geolocation error', async () => {
    const mockGeolocation = {
      getCurrentPosition: vi.fn((_, error) => {
        error(new Error('User denied'));
      }),
    };
    Object.defineProperty(navigator, 'geolocation', {
      value: mockGeolocation,
      writable: true,
    });

    const user = userEvent.setup();
    render(<MeteoPage />);

    // Wait for initial load to complete
    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(1);
    });

    const geoButton = screen.getByText('Me localiser');
    await user.click(geoButton);

    await waitFor(() => {
      expect(screen.getByText("Impossible d'obtenir votre position.")).toBeInTheDocument();
    });
  });

  it('handles missing geolocation support', async () => {
    Object.defineProperty(navigator, 'geolocation', {
      value: undefined,
      writable: true,
    });

    const user = userEvent.setup();
    render(<MeteoPage />);

    // Wait for initial load
    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(1);
    });

    const geoButton = screen.getByText('Me localiser');
    await user.click(geoButton);

    await waitFor(() => {
      expect(screen.getByText("La géolocalisation n'est pas supportée par votre navigateur.")).toBeInTheDocument();
    });
  });

  it('handles reverseGeocode failure gracefully', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
    });

    render(<MeteoPage />);

    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalled();
    });

    // Should not crash, address just won't be displayed
    expect(screen.queryByText('Paris, France')).not.toBeInTheDocument();
  });

  it('handles analysis failure gracefully', async () => {
    mockFetchMeteoAnalysis.mockRejectedValue(new Error('Analysis unavailable'));

    render(<MeteoPage />);

    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalled();
    });

    // Should not crash or show error
    expect(screen.queryByText('Analyse indisponible')).not.toBeInTheDocument();
  });

  it('shows analysis loading state', async () => {
    mockFetchMeteoAnalysis.mockImplementation(() => new Promise(() => {}));

    render(<MeteoPage />);

    await waitFor(() => {
      expect(screen.getByText('Analyse IA en cours...')).toBeInTheDocument();
    });
  });

  it('displays map image when loaded', async () => {
    render(<MeteoPage />);

    await waitFor(() => {
      const img = screen.getByAltText('Carte météo — Température');
      expect(img).toBeInTheDocument();
      expect(img).toHaveAttribute('src', 'blob:http://localhost/fake-image');
    });
  });

  it('revokes previous object URL when loading new map', async () => {
    const user = userEvent.setup();
    render(<MeteoPage />);

    // Wait for initial load
    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(1);
    });

    // Click to reload
    await user.click(screen.getByText('Afficher'));

    await waitFor(() => {
      expect(mockFetchMeteoMap).toHaveBeenCalledTimes(2);
    });

    // URL.revokeObjectURL should have been called
    expect(mockRevokeObjectURL).toHaveBeenCalled();
  });
});

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ActivitiesPage } from './ActivitiesPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/activitiesApi', () => ({
  fetchActivitySuggestions: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';
import { fetchActivitySuggestions } from '../../api/activitiesApi';

const mockUseAuth = vi.mocked(useAuth);
const mockFetchActivitySuggestions = vi.mocked(fetchActivitySuggestions);

// Mock global fetch for reverseGeocode
const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

const mockActivitySuggestion = {
  location: 'Paris',
  summary: 'Journée idéale pour les activités de plein air.',
  activities: {
    morning: [{ name: 'Marche', description: 'Balade matinale', icon: 'walk' }],
    afternoon: [{ name: 'Piscine', description: 'Profitez de la chaleur', icon: 'pool' }],
    evening: [{ name: 'Yoga', description: 'Relaxation', icon: 'yoga' }],
  },
};

describe('ActivitiesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    mockFetchActivitySuggestions.mockResolvedValue(mockActivitySuggestion);
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ display_name: 'Paris, France' }),
    });
  });

  it('renders page title', () => {
    render(<ActivitiesPage />);
    expect(screen.getByText('Activités')).toBeInTheDocument();
  });

  it('renders latitude and longitude inputs', () => {
    render(<ActivitiesPage />);
    expect(screen.getByLabelText('Latitude')).toBeInTheDocument();
    expect(screen.getByLabelText('Longitude')).toBeInTheDocument();
  });

  it('renders action buttons', async () => {
    render(<ActivitiesPage />);

    // Wait for loading to complete
    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.getByText('Rechercher')).toBeInTheDocument();
    });
    expect(screen.getByText('Me localiser')).toBeInTheDocument();
  });

  it('loads activities on initial render', async () => {
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledWith(
        'fake-token',
        48.8566,
        2.3522,
        expect.any(String)
      );
    });
  });

  it('displays address after loading', async () => {
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(screen.getByText('Paris, France')).toBeInTheDocument();
    });
  });

  it('displays summary after loading', async () => {
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(screen.getByText('Journée idéale pour les activités de plein air.')).toBeInTheDocument();
    });
  });

  it('displays activities by time slot', async () => {
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(screen.getByText('Matin')).toBeInTheDocument();
      expect(screen.getByText('Après-midi')).toBeInTheDocument();
      expect(screen.getByText('Soir')).toBeInTheDocument();
      expect(screen.getByText('Marche')).toBeInTheDocument();
      expect(screen.getByText('Piscine')).toBeInTheDocument();
      expect(screen.getByText('Yoga')).toBeInTheDocument();
    });
  });

  it('shows loading state', async () => {
    mockFetchActivitySuggestions.mockImplementation(() => new Promise(() => {}));

    render(<ActivitiesPage />);

    expect(screen.getByText('Analyse météo et suggestions en cours...')).toBeInTheDocument();
  });

  it('displays error when loading fails', async () => {
    mockFetchActivitySuggestions.mockRejectedValue(new Error('Network error'));

    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(screen.getByText("Impossible de charger les suggestions d'activités.")).toBeInTheDocument();
    });
  });

  it('allows changing latitude value', async () => {
    const user = userEvent.setup();
    render(<ActivitiesPage />);

    const latInput = screen.getByLabelText('Latitude');
    await user.clear(latInput);
    await user.type(latInput, '45.0');

    expect(latInput).toHaveValue(45);
  });

  it('allows changing longitude value', async () => {
    const user = userEvent.setup();
    render(<ActivitiesPage />);

    const lonInput = screen.getByLabelText('Longitude');
    await user.clear(lonInput);
    await user.type(lonInput, '3.0');

    expect(lonInput).toHaveValue(3);
  });

  it('submits form and reloads activities', async () => {
    const user = userEvent.setup();
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledTimes(1);
    });

    const submitButton = screen.getByText('Rechercher');
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledTimes(2);
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
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledTimes(1);
    });

    const geoButton = screen.getByText('Me localiser');
    await user.click(geoButton);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledWith(
        'fake-token',
        45.764,
        4.8357,
        expect.any(String)
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
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledTimes(1);
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
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalledTimes(1);
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

    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalled();
    });

    expect(screen.queryByText('Paris, France')).not.toBeInTheDocument();
  });

  it('renders OpenStreetMap iframe', async () => {
    render(<ActivitiesPage />);

    await waitFor(() => {
      expect(mockFetchActivitySuggestions).toHaveBeenCalled();
    });

    await waitFor(() => {
      const iframe = screen.getByTitle('OpenStreetMap');
      expect(iframe).toBeInTheDocument();
    });
  });
});

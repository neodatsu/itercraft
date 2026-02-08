import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DashboardPage } from './DashboardPage';

// Mock recharts – jsdom does not support SVG rendering
vi.mock('recharts', () => {
  const MockChart = ({ children }: Readonly<{ children?: React.ReactNode }>) => <div data-testid="line-chart">{children}</div>;
  const MockLine = ({ dataKey }: Readonly<{ dataKey?: string }>) => <span data-testid={`line-${dataKey}`} />;
  return {
    ResponsiveContainer: ({ children }: Readonly<{ children?: React.ReactNode }>) => <div>{children}</div>,
    LineChart: MockChart,
    Line: MockLine,
    XAxis: () => null,
    YAxis: () => null,
    CartesianGrid: () => null,
    Tooltip: () => null,
    Legend: () => null,
  };
});

let mockEsInstance: { close: ReturnType<typeof vi.fn>; addEventListener: ReturnType<typeof vi.fn> };
vi.stubGlobal('EventSource', class {
  close = vi.fn();
  addEventListener = vi.fn();
  constructor() {
    mockEsInstance = this;
  }
});

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/sensorApi', () => ({
  getSensorData: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';
import { getSensorData } from '../../api/sensorApi';

const mockUseAuth = vi.mocked(useAuth);
const mockGetSensorData = vi.mocked(getSensorData);

function setupAuth(username = 'testuser') {
  mockUseAuth.mockReturnValue({
    keycloak: { token: 'fake-token', tokenParsed: { preferred_username: username } } as never,
    authenticated: true,
    initialized: true,
    ensureInit: vi.fn().mockResolvedValue(true),
  });
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    mockGetSensorData.mockResolvedValue([]);
  });

  it('renders dashboard title and username', async () => {
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
    });
    expect(screen.getByText('testuser')).toBeInTheDocument();
  });

  it('shows loading state initially', () => {
    mockGetSensorData.mockReturnValue(new Promise(() => {}));
    render(<DashboardPage />);
    expect(screen.getByText('Chargement...')).toBeInTheDocument();
  });

  it('shows empty state when no data', async () => {
    mockGetSensorData.mockResolvedValue([]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Aucune donnée capteur pour cette période.')).toBeInTheDocument();
    });
  });

  it('renders chart when data is present', async () => {
    mockGetSensorData.mockResolvedValue([
      {
        measuredAt: '2026-02-08T12:00:00Z',
        deviceName: 'meteoStation_1',
        dhtTemperature: 20.7,
        dhtHumidity: 52.0,
        ntcTemperature: 21.1,
        luminosity: 77.0,
      },
    ]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByTestId('line-chart')).toBeInTheDocument();
    });
  });

  it('renders date filter inputs', async () => {
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByLabelText('Du')).toBeInTheDocument();
    });
    expect(screen.getByLabelText('Au')).toBeInTheDocument();
  });

  it('refreshes data when date filter changes', async () => {
    const user = userEvent.setup();
    render(<DashboardPage />);
    await waitFor(() => {
      expect(mockGetSensorData).toHaveBeenCalledTimes(1);
    });

    const fromInput = screen.getByLabelText('Du');
    // fireEvent.change gives a clean atomic value, avoiding intermediate invalid dates
    await user.clear(fromInput);
    const { fireEvent } = await import('@testing-library/react');
    fireEvent.change(fromInput, { target: { value: '2026-01-01' } });

    await waitFor(() => {
      expect(mockGetSensorData).toHaveBeenCalledTimes(2);
    });
  });

  it('sets up SSE listener for sensor-data-change', async () => {
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Données capteurs')).toBeInTheDocument();
    });
    expect(mockEsInstance.addEventListener).toHaveBeenCalledWith(
      'sensor-data-change',
      expect.any(Function),
    );
  });

  it('renders fallback "Utilisateur" when no token info', async () => {
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Utilisateur')).toBeInTheDocument();
    });
  });
});

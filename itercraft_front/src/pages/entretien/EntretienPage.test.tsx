import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EntretienPage } from './EntretienPage';

class MockEventSource {
  onmessage: ((event: MessageEvent) => void) | null = null;
  close = vi.fn();
  addEventListener = vi.fn();
}
vi.stubGlobal('EventSource', MockEventSource);

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/maintenanceApi', () => ({
  getActivities: vi.fn(),
  getTotals: vi.fn(),
  getHistory: vi.fn(),
  startActivity: vi.fn(),
  stopActivity: vi.fn(),
  deleteSession: vi.fn(),
  createActivity: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';
import * as api from '../../api/maintenanceApi';

const mockUseAuth = vi.mocked(useAuth);
const mockGetActivities = vi.mocked(api.getActivities);
const mockGetTotals = vi.mocked(api.getTotals);
const mockGetHistory = vi.mocked(api.getHistory);
const mockStartActivity = vi.mocked(api.startActivity);
const mockStopActivity = vi.mocked(api.stopActivity);
const mockDeleteSession = vi.mocked(api.deleteSession);
const mockCreateActivity = vi.mocked(api.createActivity);

function setupAuth() {
  mockUseAuth.mockReturnValue({
    keycloak: { token: 'fake-token', tokenParsed: { preferred_username: 'testuser' } } as never,
    authenticated: true,
    initialized: true,
    ensureInit: vi.fn().mockResolvedValue(true),
  });
}

describe('EntretienPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    mockGetActivities.mockResolvedValue([]);
    mockGetTotals.mockResolvedValue({
      todayMinutes: 0,
      weekMinutes: 0,
      monthMinutes: 0,
      yearMinutes: 0,
      byActivity: [],
    });
    mockGetHistory.mockResolvedValue([]);
  });

  it('renders page title', async () => {
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Entretien')).toBeInTheDocument();
    });
  });

  it('renders activity cards', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 30 },
      { serviceCode: 'karcher', serviceLabel: 'Passer le Karcher', isActive: false, startedAt: null, totalMinutesToday: 0 },
    ]);
    render(<EntretienPage />);
    await waitFor(() => {
      // Text appears in both activity card and history accordion
      expect(screen.getAllByText('Passer la tondeuse')).toHaveLength(2);
      expect(screen.getAllByText('Passer le Karcher')).toHaveLength(2);
    });
  });

  it('shows start button for inactive activities', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 0 },
    ]);
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Démarrer')).toBeInTheDocument();
    });
  });

  it('shows stop button for active activities', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: true, startedAt: new Date().toISOString(), totalMinutesToday: 0 },
    ]);
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Stop')).toBeInTheDocument();
    });
  });

  it('calls startActivity when clicking Démarrer', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 0 },
    ]);
    mockStartActivity.mockResolvedValue({
      id: 'session-1',
      serviceCode: 'tondeuse',
      serviceLabel: 'Passer la tondeuse',
      startedAt: new Date().toISOString(),
      endedAt: null,
      durationMinutes: null,
      autoStopped: false,
    });
    const user = userEvent.setup();

    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Démarrer')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Démarrer'));
    expect(mockStartActivity).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('calls stopActivity when clicking Stop', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: true, startedAt: new Date().toISOString(), totalMinutesToday: 0 },
    ]);
    mockStopActivity.mockResolvedValue({
      id: 'session-1',
      serviceCode: 'tondeuse',
      serviceLabel: 'Passer la tondeuse',
      startedAt: new Date().toISOString(),
      endedAt: new Date().toISOString(),
      durationMinutes: 30,
      autoStopped: false,
    });
    const user = userEvent.setup();

    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Stop')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Stop'));
    expect(mockStopActivity).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('renders totals section with per-activity breakdown', async () => {
    mockGetTotals.mockResolvedValue({
      todayMinutes: 60,
      weekMinutes: 300,
      monthMinutes: 1200,
      yearMinutes: 5000,
      byActivity: [
        { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', todayMinutes: 30, weekMinutes: 150, monthMinutes: 600, yearMinutes: 2500 },
        { serviceCode: 'karcher', serviceLabel: 'Passer le Karcher', todayMinutes: 30, weekMinutes: 150, monthMinutes: 600, yearMinutes: 2500 },
      ],
    });
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Temps total')).toBeInTheDocument();
      expect(screen.getByText('1h00')).toBeInTheDocument();
      expect(screen.getByText('Total')).toBeInTheDocument();
    });
  });

  it('renders history section with accordion', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 0 },
    ]);
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Historique')).toBeInTheDocument();
    });
  });

  it('displays today duration in activity card', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 90 },
    ]);
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Aujourd\'hui : 1h30')).toBeInTheDocument();
    });
  });

  it('loads history when accordion is opened', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 0 },
    ]);
    mockGetHistory.mockResolvedValue([
      {
        id: 'session-1',
        serviceCode: 'tondeuse',
        serviceLabel: 'Passer la tondeuse',
        startedAt: '2026-02-06T10:00:00Z',
        endedAt: '2026-02-06T11:30:00Z',
        durationMinutes: 90,
        autoStopped: false,
      },
    ]);
    const user = userEvent.setup();

    render(<EntretienPage />);
    await waitFor(() => {
      // Text appears in both activity card (h3) and history accordion (span)
      expect(screen.getAllByText('Passer la tondeuse')).toHaveLength(2);
    });

    const accordionHeaders = screen.getAllByRole('button');
    await user.click(accordionHeaders[accordionHeaders.length - 1]);

    await waitFor(() => {
      expect(mockGetHistory).toHaveBeenCalledWith('fake-token', 'tondeuse');
    });
  });

  it('calls deleteSession when clicking Supprimer', async () => {
    mockGetActivities.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', isActive: false, startedAt: null, totalMinutesToday: 0 },
    ]);
    mockGetHistory.mockResolvedValue([
      {
        id: 'session-1',
        serviceCode: 'tondeuse',
        serviceLabel: 'Passer la tondeuse',
        startedAt: '2026-02-06T10:00:00Z',
        endedAt: '2026-02-06T11:30:00Z',
        durationMinutes: 90,
        autoStopped: false,
      },
    ]);
    mockDeleteSession.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getAllByText('Passer la tondeuse')).toHaveLength(2);
    });

    const accordionHeaders = screen.getAllByRole('button');
    await user.click(accordionHeaders[accordionHeaders.length - 1]);

    await waitFor(() => {
      expect(screen.getByText('Supprimer')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Supprimer'));
    expect(mockDeleteSession).toHaveBeenCalledWith('fake-token', 'session-1');
  });

  it('calls createActivity when adding a new activity', async () => {
    mockCreateActivity.mockResolvedValue({
      serviceCode: 'jardinage',
      serviceLabel: 'Jardinage',
      isActive: false,
      startedAt: null,
      totalMinutesToday: 0,
    });
    const user = userEvent.setup();

    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Nouvelle activité (ex: Jardinage)')).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText('Nouvelle activité (ex: Jardinage)'), 'Jardinage');
    await user.click(screen.getByText('Ajouter'));

    expect(mockCreateActivity).toHaveBeenCalledWith('fake-token', 'Jardinage');
  });

  it('disables add button when input is empty', async () => {
    render(<EntretienPage />);
    await waitFor(() => {
      expect(screen.getByText('Ajouter')).toBeDisabled();
    });
  });
});

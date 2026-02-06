import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';

class MockEventSource {
  onmessage: ((event: MessageEvent) => void) | null = null;
  close = vi.fn();
  addEventListener = vi.fn();
}
vi.stubGlobal('EventSource', MockEventSource);

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/subscriptionApi', () => ({
  getSubscriptions: vi.fn(),
  getUsageHistory: vi.fn(),
  addUsage: vi.fn(),
  removeUsage: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';
import * as api from '../../api/subscriptionApi';

const mockUseAuth = vi.mocked(useAuth);
const mockGetSubscriptions = vi.mocked(api.getSubscriptions);
const mockGetUsageHistory = vi.mocked(api.getUsageHistory);
const mockAddUsage = vi.mocked(api.addUsage);
const mockRemoveUsage = vi.mocked(api.removeUsage);

function setupAuth(username = 'testuser') {
  mockUseAuth.mockReturnValue({
    keycloak: { token: 'fake-token', tokenParsed: { preferred_username: username } } as never,
    authenticated: true,
    initialized: true,
    ensureInit: vi.fn().mockResolvedValue(true),
  });
}

function renderWithRouter(component: React.ReactNode) {
  return render(<BrowserRouter>{component}</BrowserRouter>);
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    mockGetSubscriptions.mockResolvedValue([]);
    mockGetUsageHistory.mockResolvedValue([]);
  });

  it('renders username and dashboard title', async () => {
    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('testuser')).toBeInTheDocument();
    });
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
  });

  it('renders fallback "Utilisateur" when no token info', async () => {
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Utilisateur')).toBeInTheDocument();
    });
  });

  it('shows empty message when no subscriptions', async () => {
    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Aucun abonnement pour le moment.')).toBeInTheDocument();
    });
  });

  it('renders subscription section with service label', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', usageCount: 3 },
    ]);
    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Passer la tondeuse')).toBeInTheDocument();
    });
  });

  it('calls addUsage when clicking + Usage', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', usageCount: 1 },
    ]);
    mockAddUsage.mockResolvedValue(undefined);
    const user = userEvent.setup();

    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Passer la tondeuse')).toBeInTheDocument();
    });

    await user.click(screen.getByText('+ Usage'));
    expect(mockAddUsage).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('calls removeUsage with usage id when clicking Supprimer on a usage row', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', usageCount: 1 },
    ]);
    mockGetUsageHistory.mockResolvedValue([
      { id: 'usage-123', usedAt: '2026-01-15T10:30:00Z' },
    ]);
    mockRemoveUsage.mockResolvedValue(undefined);
    const user = userEvent.setup();

    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Passer la tondeuse')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('Supprimer')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Supprimer'));
    expect(mockRemoveUsage).toHaveBeenCalledWith('fake-token', 'tondeuse', 'usage-123');
  });

  it('renders usage history table with dates', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Passer la tondeuse', usageCount: 2 },
    ]);
    mockGetUsageHistory.mockResolvedValue([
      { id: 'u1', usedAt: '2026-01-15T10:30:00Z' },
      { id: 'u2', usedAt: '2025-06-20T14:00:00Z' },
    ]);
    renderWithRouter(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Passer la tondeuse')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('2 usages')).toBeInTheDocument();
    });
  });
});

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DashboardPage } from './DashboardPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/subscriptionApi', () => ({
  getSubscriptions: vi.fn(),
  getServices: vi.fn(),
  getUsageHistory: vi.fn(),
  subscribe: vi.fn(),
  unsubscribe: vi.fn(),
  addUsage: vi.fn(),
  removeUsage: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';
import * as api from '../../api/subscriptionApi';

const mockUseAuth = vi.mocked(useAuth);
const mockGetSubscriptions = vi.mocked(api.getSubscriptions);
const mockGetServices = vi.mocked(api.getServices);
const mockGetUsageHistory = vi.mocked(api.getUsageHistory);
const mockSubscribe = vi.mocked(api.subscribe);
const mockUnsubscribe = vi.mocked(api.unsubscribe);
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

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    mockGetSubscriptions.mockResolvedValue([]);
    mockGetServices.mockResolvedValue([]);
    mockGetUsageHistory.mockResolvedValue([]);
  });

  it('renders username and dashboard title', async () => {
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('testuser')).toBeInTheDocument();
    });
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders fallback "User" when no token info', async () => {
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('User')).toBeInTheDocument();
    });
  });

  it('shows empty message when no subscriptions', async () => {
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('No subscriptions yet.')).toBeInTheDocument();
    });
  });

  it('renders subscription section with service label', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 3 },
    ]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });
  });

  it('shows available services to subscribe', async () => {
    mockGetServices.mockResolvedValue([
      { code: 'piscine', label: 'Piscine', description: 'desc' },
    ]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Piscine')).toBeInTheDocument();
    });
  });

  it('calls subscribe when adding a service', async () => {
    mockGetServices.mockResolvedValue([
      { code: 'piscine', label: 'Piscine', description: 'desc' },
    ]);
    mockSubscribe.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Piscine')).toBeInTheDocument();
    });

    await user.selectOptions(screen.getByRole('combobox'), 'piscine');
    await user.click(screen.getByText('Subscribe'));

    expect(mockSubscribe).toHaveBeenCalledWith('fake-token', 'piscine');
  });

  it('calls unsubscribe when clicking Unsubscribe', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 0 },
    ]);
    mockUnsubscribe.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Unsubscribe'));
    expect(mockUnsubscribe).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('calls addUsage when clicking + Usage', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 1 },
    ]);
    mockAddUsage.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });

    await user.click(screen.getByText('+ Usage'));
    expect(mockAddUsage).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('calls removeUsage with usage id when clicking Delete on a usage row', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 1 },
    ]);
    mockGetUsageHistory.mockResolvedValue([
      { id: 'usage-123', usedAt: '2026-01-15T10:30:00Z' },
    ]);
    mockRemoveUsage.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Delete'));
    expect(mockRemoveUsage).toHaveBeenCalledWith('fake-token', 'tondeuse', 'usage-123');
  });

  it('renders usage history table with dates', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 2 },
    ]);
    mockGetUsageHistory.mockResolvedValue([
      { id: 'u1', usedAt: '2026-01-15T10:30:00Z' },
      { id: 'u2', usedAt: '2025-06-20T14:00:00Z' },
    ]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('2 usages')).toBeInTheDocument();
    });
  });
});

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DashboardPage } from './DashboardPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/subscriptionApi', () => ({
  getSubscriptions: vi.fn(),
  getServices: vi.fn(),
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
const mockSubscribe = vi.mocked(api.subscribe);
const mockUnsubscribe = vi.mocked(api.unsubscribe);
const mockAddUsage = vi.mocked(api.addUsage);
const mockRemoveUsage = vi.mocked(api.removeUsage);

function setupAuth(username = 'testuser') {
  mockUseAuth.mockReturnValue({
    keycloak: { token: 'fake-token', tokenParsed: { preferred_username: username } } as never,
    authenticated: true,
  });
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    mockGetSubscriptions.mockResolvedValue([]);
    mockGetServices.mockResolvedValue([]);
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

  it('renders subscriptions table', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 3 },
    ]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });
    expect(screen.getByText('3')).toBeInTheDocument();
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

  it('calls unsubscribe when removing a service', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 0 },
    ]);
    mockUnsubscribe.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Remove'));
    expect(mockUnsubscribe).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('calls addUsage when clicking +', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 1 },
    ]);
    mockAddUsage.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });

    await user.click(screen.getByText('+'));
    expect(mockAddUsage).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('calls removeUsage when clicking -', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 2 },
    ]);
    mockRemoveUsage.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });

    await user.click(screen.getByText('-'));
    expect(mockRemoveUsage).toHaveBeenCalledWith('fake-token', 'tondeuse');
  });

  it('disables - button when usageCount is 0', async () => {
    mockGetSubscriptions.mockResolvedValue([
      { serviceCode: 'tondeuse', serviceLabel: 'Tondeuse', usageCount: 0 },
    ]);
    render(<DashboardPage />);
    await waitFor(() => {
      expect(screen.getByText('Tondeuse')).toBeInTheDocument();
    });

    expect(screen.getByText('-')).toBeDisabled();
  });
});

import { render, screen } from '@testing-library/react';
import { DashboardPage } from './DashboardPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';

const mockUseAuth = vi.mocked(useAuth);

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
  });

  it('renders username and dashboard title', () => {
    render(<DashboardPage />);
    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
  });

  it('renders fallback "Utilisateur" when no token info', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<DashboardPage />);
    expect(screen.getByText('Utilisateur')).toBeInTheDocument();
  });
});

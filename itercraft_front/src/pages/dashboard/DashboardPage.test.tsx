import { render, screen } from '@testing-library/react';
import { DashboardPage } from './DashboardPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';

const mockUseAuth = vi.mocked(useAuth);

describe('DashboardPage', () => {
  it('renders username from preferred_username', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { tokenParsed: { preferred_username: 'healthcheck' } } as never,
      authenticated: true,
    });
    render(<DashboardPage />);
    expect(screen.getByText('healthcheck')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders name when preferred_username is absent', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { tokenParsed: { name: 'John Doe' } } as never,
      authenticated: true,
    });
    render(<DashboardPage />);
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('renders fallback "User" when no token info', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { tokenParsed: {} } as never,
      authenticated: true,
    });
    render(<DashboardPage />);
    expect(screen.getByText('User')).toBeInTheDocument();
  });
});

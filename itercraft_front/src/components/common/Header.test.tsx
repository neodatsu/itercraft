import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Header } from './Header';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../../auth/AuthProvider';

const mockUseAuth = vi.mocked(useAuth);

describe('Header', () => {
  it('renders Logout button when authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { logout: vi.fn() } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Logout')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders Login button when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { login: vi.fn() } as never,
      authenticated: false,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  it('calls keycloak.logout on Logout click', async () => {
    const logout = vi.fn();
    mockUseAuth.mockReturnValue({
      keycloak: { logout } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    await userEvent.click(screen.getByText('Logout'));
    expect(logout).toHaveBeenCalled();
  });

  it('Login link points to /dashboard', () => {
    mockUseAuth.mockReturnValue({
      keycloak: {} as never,
      authenticated: false,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Login').closest('a')).toHaveAttribute('href', '/dashboard');
  });
});

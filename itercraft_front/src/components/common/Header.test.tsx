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
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Logout')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders Login button when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { login: vi.fn() } as never,
      authenticated: false,
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  it('calls keycloak.logout on Logout click', async () => {
    const logout = vi.fn();
    mockUseAuth.mockReturnValue({
      keycloak: { logout } as never,
      authenticated: true,
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    await userEvent.click(screen.getByText('Logout'));
    expect(logout).toHaveBeenCalled();
  });

  it('calls keycloak.login on Login click', async () => {
    const login = vi.fn();
    mockUseAuth.mockReturnValue({
      keycloak: { login } as never,
      authenticated: false,
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    await userEvent.click(screen.getByText('Login'));
    expect(login).toHaveBeenCalled();
  });
});

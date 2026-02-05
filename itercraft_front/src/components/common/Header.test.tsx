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
  it('renders Déconnexion button when authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { logout: vi.fn() } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Déconnexion')).toBeInTheDocument();
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
    expect(screen.getByText('Activités')).toBeInTheDocument();
    expect(screen.getByText('Activités').closest('a')).toHaveAttribute('href', '/activites');
  });

  it('renders Architecture link always', () => {
    mockUseAuth.mockReturnValue({
      keycloak: {} as never,
      authenticated: false,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Architecture')).toBeInTheDocument();
    expect(screen.getByText('Architecture').closest('a')).toHaveAttribute('href', '/architecture');
    expect(screen.getByText('SSE')).toBeInTheDocument();
    expect(screen.getByText('SSE').closest('a')).toHaveAttribute('href', '/sse');
  });

  it('renders Connexion button when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: { login: vi.fn() } as never,
      authenticated: false,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Connexion')).toBeInTheDocument();
    expect(screen.queryByText('Activités')).not.toBeInTheDocument();
  });

  it('calls keycloak.logout on Déconnexion click', async () => {
    const logout = vi.fn();
    mockUseAuth.mockReturnValue({
      keycloak: { logout } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    await userEvent.click(screen.getByText('Déconnexion'));
    expect(logout).toHaveBeenCalled();
  });

  it('Connexion link points to /dashboard', () => {
    mockUseAuth.mockReturnValue({
      keycloak: {} as never,
      authenticated: false,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
    render(<MemoryRouter><Header /></MemoryRouter>);
    expect(screen.getByText('Connexion').closest('a')).toHaveAttribute('href', '/dashboard');
  });
});

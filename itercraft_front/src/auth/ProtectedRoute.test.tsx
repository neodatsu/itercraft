import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from './ProtectedRoute';

vi.mock('./AuthProvider', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from './AuthProvider';

const mockUseAuth = vi.mocked(useAuth);

const ensureInit = vi.fn().mockResolvedValue(true);

describe('ProtectedRoute', () => {
  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: {} as never,
      authenticated: true,
      initialized: true,
      ensureInit,
    });
    render(<ProtectedRoute><div>Secret content</div></ProtectedRoute>);
    expect(screen.getByText('Secret content')).toBeInTheDocument();
  });

  it('calls keycloak.login and renders nothing when not authenticated', () => {
    const login = vi.fn();
    mockUseAuth.mockReturnValue({
      keycloak: { login } as never,
      authenticated: false,
      initialized: true,
      ensureInit,
    });
    const { container } = render(<ProtectedRoute><div>Secret</div></ProtectedRoute>);
    expect(login).toHaveBeenCalled();
    expect(container.innerHTML).toBe('');
  });

  it('renders nothing while not initialized', () => {
    mockUseAuth.mockReturnValue({
      keycloak: {} as never,
      authenticated: false,
      initialized: false,
      ensureInit,
    });
    const { container } = render(<ProtectedRoute><div>Secret</div></ProtectedRoute>);
    expect(container.innerHTML).toBe('');
  });
});

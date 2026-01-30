import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from './ProtectedRoute';

vi.mock('./AuthProvider', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from './AuthProvider';

const mockUseAuth = vi.mocked(useAuth);

describe('ProtectedRoute', () => {
  it('renders children when authenticated', () => {
    mockUseAuth.mockReturnValue({
      keycloak: {} as never,
      authenticated: true,
    });
    render(<ProtectedRoute><div>Secret content</div></ProtectedRoute>);
    expect(screen.getByText('Secret content')).toBeInTheDocument();
  });

  it('calls keycloak.login and renders nothing when not authenticated', () => {
    const login = vi.fn();
    mockUseAuth.mockReturnValue({
      keycloak: { login } as never,
      authenticated: false,
    });
    const { container } = render(<ProtectedRoute><div>Secret</div></ProtectedRoute>);
    expect(login).toHaveBeenCalled();
    expect(container.innerHTML).toBe('');
  });
});

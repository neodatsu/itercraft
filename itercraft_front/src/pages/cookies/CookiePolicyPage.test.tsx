import { render, screen } from '@testing-library/react';
import { CookiePolicyPage } from './CookiePolicyPage';

describe('CookiePolicyPage', () => {
  it('renders title', () => {
    render(<CookiePolicyPage />);
    expect(screen.getByText('Politique de cookies')).toBeInTheDocument();
  });

  it('lists Keycloak cookies', () => {
    render(<CookiePolicyPage />);
    expect(screen.getByText('AUTH_SESSION_ID')).toBeInTheDocument();
    expect(screen.getByText('KEYCLOAK_SESSION')).toBeInTheDocument();
  });

  it('lists XSRF-TOKEN cookie', () => {
    render(<CookiePolicyPage />);
    expect(screen.getByText('XSRF-TOKEN')).toBeInTheDocument();
  });

  it('lists Google Analytics cookies', () => {
    render(<CookiePolicyPage />);
    expect(screen.getByText('_ga')).toBeInTheDocument();
    expect(screen.getByText('_ga_NMSXHLBJZK')).toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Breadcrumb } from './Breadcrumb';

function renderWithRouter(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Breadcrumb />
    </MemoryRouter>
  );
}

describe('Breadcrumb', () => {
  it('returns null on home page', () => {
    const { container } = renderWithRouter('/');
    expect(container.firstChild).toBeNull();
  });

  it('renders breadcrumb for dashboard', () => {
    renderWithRouter('/dashboard');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
  });

  it('renders breadcrumb for architecture', () => {
    renderWithRouter('/architecture');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Architecture')).toBeInTheDocument();
  });

  it('renders breadcrumb for cookies policy', () => {
    renderWithRouter('/cookies');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Politique de cookies')).toBeInTheDocument();
  });

  it('renders breadcrumb for mentions legales', () => {
    renderWithRouter('/mentions-legales');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Mentions légales')).toBeInTheDocument();
  });

  it('renders breadcrumb for privacy policy', () => {
    renderWithRouter('/confidentialite');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Confidentialité')).toBeInTheDocument();
  });

  it('renders breadcrumb for SSE page', () => {
    renderWithRouter('/sse');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Server-Sent Events')).toBeInTheDocument();
  });

  it('renders breadcrumb for meteo page', () => {
    renderWithRouter('/meteo');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('Météo')).toBeInTheDocument();
  });

  it('shows segment name for unknown paths', () => {
    renderWithRouter('/unknown-page');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('unknown-page')).toBeInTheDocument();
  });

  it('renders link for non-current items', () => {
    renderWithRouter('/dashboard');

    const homeLink = screen.getByRole('link', { name: 'Accueil' });
    expect(homeLink).toHaveAttribute('href', '/');
  });

  it('renders current item as span (not link)', () => {
    renderWithRouter('/dashboard');

    const dashboardText = screen.getByText('Tableau de bord');
    expect(dashboardText.tagName).toBe('SPAN');
    expect(dashboardText).toHaveClass('breadcrumb-current');
  });

  it('renders separator between items', () => {
    renderWithRouter('/dashboard');

    expect(screen.getByText('/')).toBeInTheDocument();
  });

  it('has aria-label for accessibility', () => {
    renderWithRouter('/dashboard');

    const nav = screen.getByRole('navigation');
    expect(nav).toHaveAttribute('aria-label', 'Breadcrumb');
  });

  it('handles nested paths correctly', () => {
    renderWithRouter('/some/nested/path');

    expect(screen.getByText('Accueil')).toBeInTheDocument();
    expect(screen.getByText('some')).toBeInTheDocument();
    expect(screen.getByText('nested')).toBeInTheDocument();
    expect(screen.getByText('path')).toBeInTheDocument();
  });
});

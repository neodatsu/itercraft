import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PrivacyPolicyPage } from './PrivacyPolicyPage';

describe('PrivacyPolicyPage', () => {
  it('renders title', () => {
    render(<PrivacyPolicyPage />, { wrapper: MemoryRouter });
    expect(screen.getByRole('heading', { level: 1, name: 'Politique de confidentialité' })).toBeInTheDocument();
  });

  it('lists sub-processors', () => {
    render(<PrivacyPolicyPage />, { wrapper: MemoryRouter });
    expect(screen.getByText('Amazon Web Services (AWS)')).toBeInTheDocument();
    expect(screen.getByText('Cloudflare')).toBeInTheDocument();
  });

  it('lists user rights', () => {
    render(<PrivacyPolicyPage />, { wrapper: MemoryRouter });
    expect(screen.getByText(/Droit d'accès/)).toBeInTheDocument();
    expect(screen.getByText(/Droit à l'effacement/)).toBeInTheDocument();
  });

  it('links to CNIL', () => {
    render(<PrivacyPolicyPage />, { wrapper: MemoryRouter });
    expect(screen.getByRole('link', { name: /cnil/i })).toHaveAttribute('href', 'https://www.cnil.fr');
  });
});

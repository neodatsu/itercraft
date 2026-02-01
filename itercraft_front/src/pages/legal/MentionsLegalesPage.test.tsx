import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { MentionsLegalesPage } from './MentionsLegalesPage';

describe('MentionsLegalesPage', () => {
  it('renders title', () => {
    render(<MentionsLegalesPage />, { wrapper: MemoryRouter });
    expect(screen.getByText('Mentions légales')).toBeInTheDocument();
  });

  it('mentions AWS hosting', () => {
    render(<MentionsLegalesPage />, { wrapper: MemoryRouter });
    expect(screen.getByText(/Amazon Web Services/)).toBeInTheDocument();
  });

  it('links to privacy and cookie policies', () => {
    render(<MentionsLegalesPage />, { wrapper: MemoryRouter });
    expect(screen.getByRole('link', { name: /confidentialité/i })).toHaveAttribute('href', '/confidentialite');
    expect(screen.getByRole('link', { name: /cookies/i })).toHaveAttribute('href', '/cookies');
  });
});

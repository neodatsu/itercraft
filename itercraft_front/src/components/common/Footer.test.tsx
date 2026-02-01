import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Footer } from './Footer';

describe('Footer', () => {
  it('renders current year and site name', () => {
    render(<Footer />, { wrapper: MemoryRouter });
    const year = new Date().getFullYear().toString();
    expect(screen.getByText(new RegExp(`${year}.*Itercraft`))).toBeInTheDocument();
  });

  it('renders legal links', () => {
    render(<Footer />, { wrapper: MemoryRouter });
    expect(screen.getByRole('link', { name: /mentions légales/i })).toHaveAttribute('href', '/mentions-legales');
    expect(screen.getByRole('link', { name: /confidentialité/i })).toHaveAttribute('href', '/confidentialite');
    expect(screen.getByRole('link', { name: /cookies/i })).toHaveAttribute('href', '/cookies');
  });
});

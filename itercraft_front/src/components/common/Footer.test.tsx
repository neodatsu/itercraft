import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Footer } from './Footer';

describe('Footer', () => {
  it('renders current year and site name', () => {
    render(<Footer />, { wrapper: MemoryRouter });
    const year = new Date().getFullYear().toString();
    expect(screen.getByText(new RegExp(`${year}.*Itercraft`))).toBeInTheDocument();
  });

  it('renders cookie policy link', () => {
    render(<Footer />, { wrapper: MemoryRouter });
    const link = screen.getByRole('link', { name: /politique de cookies/i });
    expect(link).toHaveAttribute('href', '/cookies');
  });
});

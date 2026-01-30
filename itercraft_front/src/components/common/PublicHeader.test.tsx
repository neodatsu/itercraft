import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PublicHeader } from './PublicHeader';

describe('PublicHeader', () => {
  it('renders logo link', () => {
    render(<MemoryRouter><PublicHeader /></MemoryRouter>);
    const logo = screen.getByText('itercraft');
    expect(logo).toBeInTheDocument();
    expect(logo.closest('a')).toHaveAttribute('href', '/');
  });

  it('renders Login link pointing to /dashboard', () => {
    render(<MemoryRouter><PublicHeader /></MemoryRouter>);
    const login = screen.getByText('Login');
    expect(login).toBeInTheDocument();
    expect(login.closest('a')).toHaveAttribute('href', '/dashboard');
  });
});

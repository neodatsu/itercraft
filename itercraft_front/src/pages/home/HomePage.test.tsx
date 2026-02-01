import { render, screen } from '@testing-library/react';
import { HomePage } from './HomePage';

describe('HomePage', () => {
  it('renders welcome title', () => {
    render(<HomePage />);
    expect(screen.getByText('Itercraft')).toBeInTheDocument();
  });

  it('renders philosophy section', () => {
    render(<HomePage />);
    expect(screen.getByText(/Philosophie/)).toBeInTheDocument();
  });

  it('renders principles cards', () => {
    render(<HomePage />);
    expect(screen.getByText(/Software Craftsmanship/)).toBeInTheDocument();
    expect(screen.getByText(/Infrastructure as Code/)).toBeInTheDocument();
    expect(screen.getByText(/Pipeline CI\/CD/)).toBeInTheDocument();
    expect(screen.getByText(/Sécurité by Design/)).toBeInTheDocument();
  });

  it('renders tech stack section', () => {
    render(<HomePage />);
    expect(screen.getByText('Backend')).toBeInTheDocument();
    expect(screen.getByText('Frontend')).toBeInTheDocument();
    expect(screen.getByText('Auth')).toBeInTheDocument();
    expect(screen.getByText('Cloud')).toBeInTheDocument();
  });
});

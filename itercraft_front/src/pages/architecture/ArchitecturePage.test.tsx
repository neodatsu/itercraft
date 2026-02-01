import { render, screen } from '@testing-library/react';
import { ArchitecturePage } from './ArchitecturePage';

vi.mock('mermaid', () => ({
  default: {
    initialize: vi.fn(),
    run: vi.fn(),
  },
}));

describe('ArchitecturePage', () => {
  it('renders page title', () => {
    render(<ArchitecturePage />);
    expect(screen.getByText('Architecture')).toBeInTheDocument();
  });

  it('renders context section', () => {
    render(<ArchitecturePage />);
    expect(screen.getByText('Contexte (C4 Level 1)')).toBeInTheDocument();
  });

  it('renders container section', () => {
    render(<ArchitecturePage />);
    expect(screen.getByText('Conteneurs (C4 Level 2)')).toBeInTheDocument();
  });
});

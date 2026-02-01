import { render, screen } from '@testing-library/react';
import { SseDiagramsPage } from './SseDiagramsPage';

vi.mock('mermaid', () => ({
  default: {
    initialize: vi.fn(),
    run: vi.fn(),
  },
}));

describe('SseDiagramsPage', () => {
  it('renders page title', () => {
    render(<SseDiagramsPage />);
    expect(screen.getByText('Server-Sent Events (SSE)')).toBeInTheDocument();
  });

  it('renders all four diagram sections', () => {
    render(<SseDiagramsPage />);
    expect(screen.getByText('1. Connexion SSE')).toBeInTheDocument();
    expect(screen.getByText('2. Abonnement à un service')).toBeInTheDocument();
    expect(screen.getByText("3. Ajout d'un usage")).toBeInTheDocument();
    expect(screen.getByText('4. Déconnexion et nettoyage')).toBeInTheDocument();
  });
});

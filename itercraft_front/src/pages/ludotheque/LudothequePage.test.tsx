import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LudothequePage } from './LudothequePage';

class MockEventSource {
  onmessage: ((event: MessageEvent) => void) | null = null;
  close = vi.fn();
  addEventListener = vi.fn();
}
vi.stubGlobal('EventSource', MockEventSource);

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/ludothequeApi', () => ({
  getMesJeux: vi.fn(),
  getReferences: vi.fn(),
  addJeuByTitle: vi.fn(),
  removeJeu: vi.fn(),
  updateNote: vi.fn(),
  getSuggestion: vi.fn(),
  getSseUrl: vi.fn(() => 'http://test/api/events'),
}));

import { useAuth } from '../../auth/AuthProvider';
import * as api from '../../api/ludothequeApi';

const mockUseAuth = vi.mocked(useAuth);
const mockGetMesJeux = vi.mocked(api.getMesJeux);
const mockGetReferences = vi.mocked(api.getReferences);
const mockAddJeuByTitle = vi.mocked(api.addJeuByTitle);
const mockRemoveJeu = vi.mocked(api.removeJeu);
const mockUpdateNote = vi.mocked(api.updateNote);
const mockGetSuggestion = vi.mocked(api.getSuggestion);

function setupAuth() {
  mockUseAuth.mockReturnValue({
    keycloak: { token: 'fake-token', tokenParsed: { preferred_username: 'testuser' } } as never,
    authenticated: true,
    initialized: true,
    ensureInit: vi.fn().mockResolvedValue(true),
  });
}

const mockJeuUser = {
  id: 'ju-1',
  jeu: {
    id: 'jeu-1',
    nom: 'Catan',
    description: 'Jeu de stratégie',
    typeCode: 'strategie',
    typeLibelle: 'Stratégie',
    joueursMin: 3,
    joueursMax: 4,
    ageCode: 'tout_public',
    ageLibelle: 'Tout public',
    dureeMoyenneMinutes: 90,
    complexiteNiveau: 3,
    complexiteLibelle: 'Intermédiaire',
    imageUrl: null,
  },
  note: 4,
  createdAt: '2026-01-15T10:00:00Z',
};

const mockReferences = {
  types: [{ id: 't1', code: 'strategie', libelle: 'Stratégie' }],
  ages: [{ id: 'a1', code: 'tout_public', libelle: 'Tout public', ageMinimum: 10 }],
  complexites: [{ id: 'c1', niveau: 3, libelle: 'Intermédiaire' }],
};

describe('LudothequePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    mockGetMesJeux.mockResolvedValue([]);
    mockGetReferences.mockResolvedValue(mockReferences);
  });

  it('renders page title', async () => {
    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Ma Ludothèque')).toBeInTheDocument();
    });
  });

  it('shows empty message when no games', async () => {
    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText(/ludothèque est vide/)).toBeInTheDocument();
    });
  });

  it('renders games in table', async () => {
    mockGetMesJeux.mockResolvedValue([mockJeuUser]);
    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Catan')).toBeInTheDocument();
    });
    // Stratégie appears in filter dropdown and table badge
    expect(screen.getAllByText('Stratégie').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('3-4')).toBeInTheDocument();
    expect(screen.getByText('90 min')).toBeInTheDocument();
  });

  it('filters games by name', async () => {
    const user = userEvent.setup();
    mockGetMesJeux.mockResolvedValue([
      mockJeuUser,
      { ...mockJeuUser, id: 'ju-2', jeu: { ...mockJeuUser.jeu, id: 'jeu-2', nom: 'Dixit' } },
    ]);

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Catan')).toBeInTheDocument();
      expect(screen.getByText('Dixit')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/nom du jeu/i);
    await user.type(searchInput, 'cat');

    expect(screen.getByText('Catan')).toBeInTheDocument();
    expect(screen.queryByText('Dixit')).not.toBeInTheDocument();
  });

  it('opens add game modal', async () => {
    const user = userEvent.setup();
    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('+ Ajouter un jeu')).toBeInTheDocument();
    });

    await user.click(screen.getByText('+ Ajouter un jeu'));

    expect(screen.getByText('Ajouter un jeu')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/catan/i)).toBeInTheDocument();
  });

  it('calls addJeuByTitle when submitting add form', async () => {
    const user = userEvent.setup();
    const newJeu = { ...mockJeuUser, id: 'new-ju', jeu: { ...mockJeuUser.jeu, id: 'new-jeu', nom: 'Nouveau Jeu' } };
    mockAddJeuByTitle.mockResolvedValue(newJeu);

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('+ Ajouter un jeu')).toBeInTheDocument();
    });

    await user.click(screen.getByText('+ Ajouter un jeu'));
    await user.type(screen.getByPlaceholderText(/catan/i), 'Nouveau Jeu');
    await user.click(screen.getByRole('button', { name: 'Ajouter' }));

    expect(mockAddJeuByTitle).toHaveBeenCalledWith('fake-token', 'Nouveau Jeu');
  });

  it('calls removeJeu when clicking Retirer', async () => {
    const user = userEvent.setup();
    mockGetMesJeux.mockResolvedValue([mockJeuUser]);
    mockRemoveJeu.mockResolvedValue(undefined);

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Catan')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /retirer catan/i }));

    expect(mockRemoveJeu).toHaveBeenCalledWith('fake-token', 'jeu-1');
  });

  it('calls updateNote when clicking stars', async () => {
    const user = userEvent.setup();
    mockGetMesJeux.mockResolvedValue([{ ...mockJeuUser, note: null }]);
    mockUpdateNote.mockResolvedValue(undefined);

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Catan')).toBeInTheDocument();
    });

    // Find the star buttons in the table (not in filter)
    const starButtons = screen.getAllByRole('button', { name: '3 étoiles' });
    await user.click(starButtons[starButtons.length - 1]); // Click the one in the table row

    expect(mockUpdateNote).toHaveBeenCalledWith('fake-token', 'jeu-1', 3);
  });

  it('opens suggestion modal and shows loading', async () => {
    const user = userEvent.setup();
    mockGetSuggestion.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Proposition IA')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Proposition IA'));

    expect(screen.getByText('Suggestion IA')).toBeInTheDocument();
    expect(screen.getByText(/analyse de vos préférences/i)).toBeInTheDocument();
  });

  it('displays suggestion when loaded', async () => {
    const user = userEvent.setup();
    mockGetSuggestion.mockResolvedValue({
      nom: 'Terraforming Mars',
      description: 'Jeu de colonisation',
      typeCode: 'strategie',
      typeLibelle: 'Stratégie',
      raison: 'Vous aimez les jeux de stratégie',
      imageUrl: null,
    });

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Proposition IA')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Proposition IA'));

    await waitFor(() => {
      expect(screen.getByText('Terraforming Mars')).toBeInTheDocument();
    });
    expect(screen.getByText('Jeu de colonisation')).toBeInTheDocument();
    expect(screen.getByText(/vous aimez les jeux de stratégie/i)).toBeInTheDocument();
  });

  it('shows error when suggestion fails', async () => {
    const user = userEvent.setup();
    mockGetSuggestion.mockRejectedValue(new Error('Vous devez noter au moins un jeu'));

    render(<LudothequePage />);
    await waitFor(() => {
      expect(screen.getByText('Proposition IA')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Proposition IA'));

    await waitFor(() => {
      expect(screen.getByText(/noter au moins un jeu/)).toBeInTheDocument();
    });
  });
});

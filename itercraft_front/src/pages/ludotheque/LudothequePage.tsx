import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../auth/AuthProvider';
import {
  getMesJeux,
  getReferences,
  addJeuByTitle,
  removeJeu,
  updateNote,
  getSuggestion,
  getSseUrl,
  type JeuUserDto,
  type ReferenceTablesDto,
  type GameSuggestionDto,
} from '../../api/ludothequeApi';
import { StarRating } from '../../components/ludotheque/StarRating';
import './LudothequePage.css';

export function LudothequePage() {
  const { keycloak } = useAuth();
  const token = keycloak.token ?? '';

  const [jeux, setJeux] = useState<JeuUserDto[]>([]);
  const [references, setReferences] = useState<ReferenceTablesDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filtres
  const [filterNom, setFilterNom] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterAge, setFilterAge] = useState('');
  const [filterNote, setFilterNote] = useState<number | null>(null);
  const [filterDuree, setFilterDuree] = useState('');

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Modal ajout
  const [showAddModal, setShowAddModal] = useState(false);
  const [newGameTitle, setNewGameTitle] = useState('');
  const [addingGame, setAddingGame] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  // Modal suggestion
  const [showSuggestionModal, setShowSuggestionModal] = useState(false);
  const [suggestion, setSuggestion] = useState<GameSuggestionDto | null>(null);
  const [loadingSuggestion, setLoadingSuggestion] = useState(false);
  const [suggestionError, setSuggestionError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const [jeuxData, refsData] = await Promise.all([
        getMesJeux(token),
        getReferences(token),
      ]);
      setJeux(jeuxData);
      setReferences(refsData);
      setError(null);
    } catch (e) {
      setError('Erreur lors du chargement de la ludothèque');
    }
  }, [token]);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  // SSE pour les mises à jour temps réel
  useEffect(() => {
    const es = new EventSource(getSseUrl());
    es.addEventListener('ludotheque-change', () => {
      refresh();
    });
    return () => es.close();
  }, [refresh]);

  const matchesDuree = (duree: number, filter: string): boolean => {
    switch (filter) {
      case 'court': return duree <= 30;
      case 'moyen': return duree > 30 && duree <= 60;
      case 'long': return duree > 60;
      default: return true;
    }
  };

  const filteredJeux = useMemo(() => {
    return jeux.filter((ju) => {
      const matchesNom = !filterNom || ju.jeu.nom.toLowerCase().includes(filterNom.toLowerCase());
      const matchesType = !filterType || ju.jeu.typeCode === filterType;
      const matchesAge = !filterAge || ju.jeu.ageCode === filterAge;
      const matchesNote = filterNote === null || (ju.note !== null && ju.note >= filterNote);
      const matchesDureeFilter = !filterDuree || matchesDuree(ju.jeu.dureeMoyenneMinutes ?? 0, filterDuree);

      return matchesNom && matchesType && matchesAge && matchesNote && matchesDureeFilter;
    });
  }, [jeux, filterNom, filterType, filterAge, filterNote, filterDuree]);

  // Reset page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [filterNom, filterType, filterAge, filterNote, filterDuree]);

  const totalPages = Math.ceil(filteredJeux.length / pageSize);
  const paginatedJeux = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredJeux.slice(start, start + pageSize);
  }, [filteredJeux, currentPage, pageSize]);

  function handlePageSizeChange(newSize: number) {
    setPageSize(newSize);
    setCurrentPage(1);
  }

  async function handleAddGame(e: React.SyntheticEvent<HTMLFormElement, SubmitEvent>) {
    e.preventDefault();
    if (!newGameTitle.trim()) return;

    setAddingGame(true);
    setAddError(null);
    try {
      await addJeuByTitle(token, newGameTitle.trim());
      setNewGameTitle('');
      setShowAddModal(false);
      await refresh();
    } catch (err) {
      setAddError(err instanceof Error ? err.message : 'Erreur lors de l\'ajout');
    } finally {
      setAddingGame(false);
    }
  }

  async function handleRemoveGame(jeuId: string) {
    try {
      await removeJeu(token, jeuId);
      await refresh();
    } catch {
      // Erreur silencieuse, le SSE mettra à jour
    }
  }

  async function handleUpdateNote(jeuId: string, note: number) {
    try {
      await updateNote(token, jeuId, note);
      // Mise à jour optimiste
      setJeux((prev) =>
        prev.map((ju) =>
          ju.jeu.id === jeuId ? { ...ju, note } : ju
        )
      );
    } catch {
      await refresh();
    }
  }

  async function handleGetSuggestion() {
    setShowSuggestionModal(true);
    setLoadingSuggestion(true);
    setSuggestionError(null);
    setSuggestion(null);

    try {
      const result = await getSuggestion(token);
      setSuggestion(result);
    } catch (err) {
      setSuggestionError(err instanceof Error ? err.message : 'Erreur lors de la génération');
    } finally {
      setLoadingSuggestion(false);
    }
  }

  async function handleAddSuggestedGame() {
    if (!suggestion) return;
    setAddingGame(true);
    try {
      await addJeuByTitle(token, suggestion.nom);
      setShowSuggestionModal(false);
      setSuggestion(null);
      await refresh();
    } catch {
      // Peut déjà exister
    } finally {
      setAddingGame(false);
    }
  }

  function getComplexiteClass(niveau: number): string {
    if (niveau <= 1) return 'complexite-facile';
    if (niveau <= 2) return 'complexite-accessible';
    if (niveau <= 3) return 'complexite-moyen';
    if (niveau <= 4) return 'complexite-avance';
    return 'complexite-expert';
  }

  if (loading) {
    return (
      <div className="ludotheque-container">
        <div className="ludotheque-loading">Chargement de votre ludothèque...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="ludotheque-container">
        <div className="ludotheque-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="ludotheque-container">
      <header className="ludotheque-header">
        <h1>Ma Ludothèque</h1>
        <div className="ludotheque-actions">
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            + Ajouter un jeu
          </button>
          <button className="btn btn-secondary" onClick={handleGetSuggestion}>
            Proposition IA
          </button>
        </div>
      </header>

      <section className="ludotheque-filters" aria-label="Filtres">
        <div className="filter-group">
          <label htmlFor="filter-nom">Rechercher</label>
          <input
            id="filter-nom"
            type="text"
            placeholder="Nom du jeu..."
            value={filterNom}
            onChange={(e) => setFilterNom(e.target.value)}
          />
        </div>

        <div className="filter-group">
          <label htmlFor="filter-type">Type</label>
          <select
            id="filter-type"
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
          >
            <option value="">Tous</option>
            {references?.types.map((t) => (
              <option key={t.id} value={t.code}>{t.libelle}</option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="filter-age">Âge</label>
          <select
            id="filter-age"
            value={filterAge}
            onChange={(e) => setFilterAge(e.target.value)}
          >
            <option value="">Tous</option>
            {references?.ages.map((a) => (
              <option key={a.id} value={a.code}>{a.libelle}</option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="filter-duree">Durée</label>
          <select
            id="filter-duree"
            value={filterDuree}
            onChange={(e) => setFilterDuree(e.target.value)}
          >
            <option value="">Toutes</option>
            <option value="court">Court (&lt;30 min)</option>
            <option value="moyen">Moyen (30-60 min)</option>
            <option value="long">Long (&gt;60 min)</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Note min.</label>
          <div className="filter-stars">
            <StarRating
              value={filterNote}
              onChange={(v) => setFilterNote(filterNote === v ? null : v)}
            />
            {filterNote !== null && (
              <button
                className="btn-clear-filter"
                onClick={() => setFilterNote(null)}
                aria-label="Effacer filtre note"
              >
                ×
              </button>
            )}
          </div>
        </div>
      </section>

      <section className="ludotheque-content" aria-label="Liste des jeux">
        {filteredJeux.length === 0 ? (
          <div className="ludotheque-empty">
            {jeux.length === 0
              ? 'Votre ludothèque est vide. Ajoutez votre premier jeu !'
              : 'Aucun jeu ne correspond aux filtres sélectionnés.'}
          </div>
        ) : (
          <>
          <table className="ludotheque-table">
            <thead>
              <tr>
                <th className="col-image">Image</th>
                <th className="col-nom">Nom</th>
                <th className="col-type">Type</th>
                <th className="col-joueurs">Joueurs</th>
                <th className="col-duree">Durée</th>
                <th className="col-complexite">Complexité</th>
                <th className="col-note">Note</th>
                <th className="col-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginatedJeux.map((ju) => (
                <tr key={ju.id}>
                  <td className="col-image">
                    {ju.jeu.imageUrl ? (
                      <img src={ju.jeu.imageUrl} alt={ju.jeu.nom} className="game-thumbnail" />
                    ) : (
                      <div className="game-thumbnail-placeholder">🎲</div>
                    )}
                  </td>
                  <td className="col-nom">
                    <div className="game-name">{ju.jeu.nom}</div>
                    <div className="game-description">{ju.jeu.description}</div>
                  </td>
                  <td className="col-type">
                    <span className="badge badge-type">{ju.jeu.typeLibelle}</span>
                  </td>
                  <td className="col-joueurs">{ju.jeu.joueursMin}-{ju.jeu.joueursMax}</td>
                  <td className="col-duree">
                    {ju.jeu.dureeMoyenneMinutes ? `${ju.jeu.dureeMoyenneMinutes} min` : '-'}
                  </td>
                  <td className="col-complexite">
                    <span className={`badge ${getComplexiteClass(ju.jeu.complexiteNiveau)}`}>
                      {ju.jeu.complexiteLibelle}
                    </span>
                  </td>
                  <td className="col-note">
                    <StarRating
                      value={ju.note}
                      onChange={(note) => handleUpdateNote(ju.jeu.id, note)}
                    />
                  </td>
                  <td className="col-actions">
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleRemoveGame(ju.jeu.id)}
                      aria-label={`Retirer ${ju.jeu.nom}`}
                    >
                      Retirer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <div className="pagination-info">
              {filteredJeux.length} jeu{filteredJeux.length !== 1 ? 'x' : ''} trouvé{filteredJeux.length !== 1 ? 's' : ''}
            </div>
            <div className="pagination-controls">
              <button
                className="btn btn-sm"
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
              >
                ←
              </button>
              <span className="pagination-pages">
                Page {currentPage} / {totalPages || 1}
              </span>
              <button
                className="btn btn-sm"
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage >= totalPages}
              >
                →
              </button>
            </div>
            <div className="pagination-size">
              <label htmlFor="page-size">Par page :</label>
              <select
                id="page-size"
                value={pageSize}
                onChange={(e) => handlePageSizeChange(Number(e.target.value))}
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>
          </>
        )}
      </section>

      {/* Modal Ajout */}
      {showAddModal && (
        <div
          className="modal-overlay"
          onClick={() => setShowAddModal(false)}
          onKeyDown={(e) => e.key === 'Escape' && setShowAddModal(false)}
          role="presentation"
        >
          <div
            className="modal"
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-modal-title"
          >
            <h2 id="add-modal-title">Ajouter un jeu</h2>
            <form onSubmit={handleAddGame}>
              <div className="form-group">
                <label htmlFor="new-game-title">Nom du jeu</label>
                <input
                  id="new-game-title"
                  type="text"
                  value={newGameTitle}
                  onChange={(e) => setNewGameTitle(e.target.value)}
                  placeholder="Ex: Catan, 7 Wonders..."
                  disabled={addingGame}
                  autoFocus
                />
                <p className="form-hint">
                  Notre IA remplira automatiquement les détails du jeu.
                </p>
              </div>
              {addError && <div className="modal-error">{addError}</div>}
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowAddModal(false)}
                  disabled={addingGame}
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={addingGame || !newGameTitle.trim()}
                >
                  {addingGame ? 'Ajout en cours...' : 'Ajouter'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Suggestion */}
      {showSuggestionModal && (
        <div
          className="modal-overlay"
          onClick={() => setShowSuggestionModal(false)}
          onKeyDown={(e) => e.key === 'Escape' && setShowSuggestionModal(false)}
          role="presentation"
        >
          <div
            className="modal suggestion-modal"
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="suggestion-modal-title"
          >
            <h2 id="suggestion-modal-title">Suggestion IA</h2>
            {loadingSuggestion && (
              <div className="suggestion-loading">
                <div className="spinner" />
                <p>Analyse de vos préférences...</p>
              </div>
            )}
            {suggestionError && (
              <div className="modal-error">{suggestionError}</div>
            )}
            {suggestion && (
              <div className="suggestion-content">
                {suggestion.imageUrl && (
                  <img src={suggestion.imageUrl} alt={suggestion.nom} className="suggestion-image" />
                )}
                <h3>{suggestion.nom}</h3>
                <span className="badge badge-type">{suggestion.typeLibelle}</span>
                <p className="suggestion-description">{suggestion.description}</p>
                <div className="suggestion-raison">
                  <strong>Pourquoi ce jeu ?</strong>
                  <p>{suggestion.raison}</p>
                </div>
              </div>
            )}
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowSuggestionModal(false)}
              >
                Fermer
              </button>
              {suggestion && (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleAddSuggestedGame}
                  disabled={addingGame}
                >
                  {addingGame ? 'Ajout...' : 'Ajouter à ma ludothèque'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

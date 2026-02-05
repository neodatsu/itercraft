const API_URL = import.meta.env.VITE_API_URL;

function getCsrfToken(): string {
  const match = /XSRF-TOKEN=([^;]+)/.exec(document.cookie);
  return match ? decodeURIComponent(match[1]) : '';
}

function authHeaders(accessToken: string): Record<string, string> {
  return {
    Authorization: `Bearer ${accessToken}`,
    'Content-Type': 'application/json',
  };
}

function mutationHeaders(accessToken: string): Record<string, string> {
  return {
    ...authHeaders(accessToken),
    'X-XSRF-TOKEN': getCsrfToken(),
  };
}

export interface JeuDto {
  id: string;
  nom: string;
  description: string;
  typeCode: string;
  typeLibelle: string;
  joueursMin: number;
  joueursMax: number;
  ageCode: string;
  ageLibelle: string;
  dureeMoyenneMinutes: number | null;
  complexiteNiveau: number;
  complexiteLibelle: string;
  imageUrl: string | null;
}

export interface JeuUserDto {
  id: string;
  jeu: JeuDto;
  note: number | null;
  createdAt: string;
}

export interface GameSuggestionDto {
  nom: string;
  description: string;
  typeCode: string;
  typeLibelle: string;
  raison: string;
  imageUrl: string | null;
}

export interface TypeJeuRef {
  id: string;
  code: string;
  libelle: string;
}

export interface AgeJeuRef {
  id: string;
  code: string;
  libelle: string;
  ageMinimum: number | null;
}

export interface ComplexiteJeuRef {
  id: string;
  niveau: number;
  libelle: string;
}

export interface ReferenceTablesDto {
  types: TypeJeuRef[];
  ages: AgeJeuRef[];
  complexites: ComplexiteJeuRef[];
}

export async function getMesJeux(accessToken: string): Promise<JeuUserDto[]> {
  const res = await fetch(`${API_URL}/api/ludotheque/mes-jeux`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Erreur lors du chargement de votre ludothèque');
  return res.json();
}

export async function getAllJeux(accessToken: string): Promise<JeuDto[]> {
  const res = await fetch(`${API_URL}/api/ludotheque/jeux`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Erreur lors du chargement du catalogue');
  return res.json();
}

export async function getReferences(accessToken: string): Promise<ReferenceTablesDto> {
  const res = await fetch(`${API_URL}/api/ludotheque/references`, {
    headers: authHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Erreur lors du chargement des références');
  return res.json();
}

export async function addJeuByTitle(accessToken: string, nom: string): Promise<JeuUserDto> {
  const res = await fetch(`${API_URL}/api/ludotheque/mes-jeux`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
    body: JSON.stringify({ nom }),
  });
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(errorText || 'Erreur lors de l\'ajout du jeu');
  }
  return res.json();
}

export async function removeJeu(accessToken: string, jeuId: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/ludotheque/mes-jeux/${jeuId}`, {
    method: 'DELETE',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Erreur lors de la suppression du jeu');
}

export async function updateNote(accessToken: string, jeuId: string, note: number | null): Promise<void> {
  const res = await fetch(`${API_URL}/api/ludotheque/mes-jeux/${jeuId}/note`, {
    method: 'PUT',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
    body: JSON.stringify({ note }),
  });
  if (!res.ok) throw new Error('Erreur lors de la mise à jour de la note');
}

export async function getSuggestion(accessToken: string): Promise<GameSuggestionDto> {
  const res = await fetch(`${API_URL}/api/ludotheque/suggestion`, {
    method: 'POST',
    headers: mutationHeaders(accessToken),
    credentials: 'include',
  });
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(errorText || 'Erreur lors de la génération de la suggestion');
  }
  return res.json();
}

export function getSseUrl(): string {
  return `${API_URL}/api/events`;
}

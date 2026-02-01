import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { CookieConsent } from './CookieConsent';

function renderConsent() {
  return render(<CookieConsent />, { wrapper: MemoryRouter });
}

const store = new Map<string, string>();

const fakeStorage = {
  getItem: (key: string) => store.get(key) ?? null,
  setItem: (key: string, value: string) => { store.set(key, value); },
  removeItem: (key: string) => { store.delete(key); },
  clear: () => { store.clear(); },
  get length() { return store.size; },
  key: (_i: number) => null,
} as Storage;

beforeEach(() => {
  store.clear();
  vi.stubGlobal('localStorage', fakeStorage);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('CookieConsent', () => {
  it('shows banner when no choice stored', () => {
    renderConsent();
    expect(screen.getByText(/utilise des cookies/)).toBeInTheDocument();
  });

  it('hides banner when already accepted', () => {
    store.set('cookie-consent', 'accepted');
    renderConsent();
    expect(screen.queryByText(/utilise des cookies/)).not.toBeInTheDocument();
  });

  it('hides banner after clicking Accepter', async () => {
    const user = userEvent.setup();
    renderConsent();
    await user.click(screen.getByText('Accepter'));
    expect(screen.queryByText(/utilise des cookies/)).not.toBeInTheDocument();
    expect(store.get('cookie-consent')).toBe('accepted');
  });

  it('hides banner after clicking Refuser', async () => {
    const user = userEvent.setup();
    renderConsent();
    await user.click(screen.getByText('Refuser'));
    expect(screen.queryByText(/utilise des cookies/)).not.toBeInTheDocument();
    expect(store.get('cookie-consent')).toBe('refused');
  });
});

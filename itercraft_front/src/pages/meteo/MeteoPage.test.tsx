import { render, screen } from '@testing-library/react';
import { MeteoPage } from './MeteoPage';

vi.mock('../../auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../../api/meteoApi', () => ({
  LAYERS: [
    { code: 'TEMPERATURE__SPECIFIC_HEIGHT_LEVEL_ABOVE_GROUND', label: 'Température' },
    { code: 'TOTAL_PRECIPITATION__GROUND_OR_WATER_SURFACE', label: 'Précipitations' },
  ],
  fetchMeteoMap: vi.fn().mockResolvedValue('blob:http://localhost/fake'),
}));

import { useAuth } from '../../auth/AuthProvider';

const mockUseAuth = vi.mocked(useAuth);

describe('MeteoPage', () => {
  beforeEach(() => {
    mockUseAuth.mockReturnValue({
      keycloak: { token: 'fake-token', tokenParsed: {} } as never,
      authenticated: true,
      initialized: true,
      ensureInit: vi.fn().mockResolvedValue(true),
    });
  });

  it('renders page title', () => {
    render(<MeteoPage />);
    expect(screen.getByText('Météo')).toBeInTheDocument();
  });

  it('renders latitude and longitude inputs', () => {
    render(<MeteoPage />);
    expect(screen.getByLabelText('Latitude')).toBeInTheDocument();
    expect(screen.getByLabelText('Longitude')).toBeInTheDocument();
  });

  it('renders layer selector with options', () => {
    render(<MeteoPage />);
    expect(screen.getByLabelText('Couche')).toBeInTheDocument();
    expect(screen.getByText('Température')).toBeInTheDocument();
    expect(screen.getByText('Précipitations')).toBeInTheDocument();
  });

  it('renders action buttons', () => {
    render(<MeteoPage />);
    expect(screen.getByText('Afficher')).toBeInTheDocument();
    expect(screen.getByText('Me localiser')).toBeInTheDocument();
  });
});

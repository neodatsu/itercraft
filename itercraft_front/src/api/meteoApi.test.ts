import { fetchMeteoMap, fetchMeteoAnalysis, LAYERS } from './meteoApi';

// Mock global fetch
const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

// Mock URL.createObjectURL
vi.stubGlobal('URL', {
  ...URL,
  createObjectURL: vi.fn(() => 'blob:http://localhost/mock-url'),
});

// Mock document.cookie for CSRF token
Object.defineProperty(document, 'cookie', {
  writable: true,
  value: 'XSRF-TOKEN=test-csrf-token',
});

describe('meteoApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('LAYERS', () => {
    it('should contain weather layer definitions', () => {
      expect(LAYERS).toBeInstanceOf(Array);
      expect(LAYERS.length).toBeGreaterThan(0);
      expect(LAYERS[0]).toHaveProperty('code');
      expect(LAYERS[0]).toHaveProperty('label');
    });

    it('should have temperature layer', () => {
      const tempLayer = LAYERS.find(l => l.code.includes('TEMPERATURE'));
      expect(tempLayer).toBeDefined();
      expect(tempLayer?.label).toBe('Température');
    });
  });

  describe('fetchMeteoMap', () => {
    it('should fetch map and return blob URL', async () => {
      const mockBlob = new Blob(['test'], { type: 'image/png' });
      mockFetch.mockResolvedValue({
        ok: true,
        blob: () => Promise.resolve(mockBlob),
      });

      const result = await fetchMeteoMap('test-token', 'TEMPERATURE', 48.8566, 2.3522);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/meteo/map?'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            Authorization: 'Bearer test-token',
            'X-XSRF-TOKEN': 'test-csrf-token',
          }),
          credentials: 'include',
        })
      );
      expect(result).toBe('blob:http://localhost/mock-url');
    });

    it('should include correct query parameters', async () => {
      const mockBlob = new Blob(['test'], { type: 'image/png' });
      mockFetch.mockResolvedValue({
        ok: true,
        blob: () => Promise.resolve(mockBlob),
      });

      await fetchMeteoMap('token', 'WIND_SPEED', 45.0, 3.0);

      const calledUrl = mockFetch.mock.calls[0][0];
      expect(calledUrl).toContain('layer=WIND_SPEED');
      expect(calledUrl).toContain('lat=45');
      expect(calledUrl).toContain('lon=3');
      expect(calledUrl).toContain('width=512');
      expect(calledUrl).toContain('height=512');
    });

    it('should throw error on failed response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
      });

      await expect(fetchMeteoMap('token', 'TEMP', 48, 2)).rejects.toThrow(
        'Échec du chargement de la carte météo'
      );
    });
  });

  describe('fetchMeteoAnalysis', () => {
    it('should fetch analysis and return text', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ analysis: 'Temps ensoleillé sur Paris.' }),
      });

      const result = await fetchMeteoAnalysis('test-token', 'TEMPERATURE', 48.8566, 2.3522, 'Paris');

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/meteo/analyze?'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            Authorization: 'Bearer test-token',
          }),
        })
      );
      expect(result).toBe('Temps ensoleillé sur Paris.');
    });

    it('should include location in query parameters', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ analysis: 'Test' }),
      });

      await fetchMeteoAnalysis('token', 'TEMP', 45, 3, 'Lyon, France');

      const calledUrl = mockFetch.mock.calls[0][0];
      expect(calledUrl).toContain('location=Lyon');
    });

    it('should throw error on failed response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
      });

      await expect(
        fetchMeteoAnalysis('token', 'TEMP', 48, 2, 'Paris')
      ).rejects.toThrow('Analyse indisponible');
    });
  });

  describe('getCsrfToken', () => {
    it('should extract token from cookie', async () => {
      document.cookie = 'XSRF-TOKEN=my-csrf-token-123';
      mockFetch.mockResolvedValue({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      });

      await fetchMeteoMap('token', 'TEMP', 48, 2);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-XSRF-TOKEN': 'my-csrf-token-123',
          }),
        })
      );
    });

    it('should return empty string when no cookie', async () => {
      document.cookie = '';
      mockFetch.mockResolvedValue({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      });

      await fetchMeteoMap('token', 'TEMP', 48, 2);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-XSRF-TOKEN': '',
          }),
        })
      );
    });
  });
});

import { render, screen } from '@testing-library/react';
import { IotPage } from './IotPage';

vi.mock('mermaid', () => ({
  default: {
    initialize: vi.fn(),
    run: vi.fn(),
  },
}));

describe('IotPage', () => {
  it('renders page title', () => {
    render(<IotPage />);
    expect(screen.getByText('IoT — Pipeline de données capteurs')).toBeInTheDocument();
  });

  it('renders overview section', () => {
    render(<IotPage />);
    expect(screen.getByText("Vue d'ensemble")).toBeInTheDocument();
  });

  it('renders ESP32 section', () => {
    render(<IotPage />);
    expect(screen.getByText('ESP32 & Capteurs')).toBeInTheDocument();
  });

  it('renders MQTT section', () => {
    render(<IotPage />);
    expect(screen.getByText('Broker MQTT — Mosquitto')).toBeInTheDocument();
  });

  it('renders backend section', () => {
    render(<IotPage />);
    expect(screen.getByText('Backend — Spring Boot')).toBeInTheDocument();
  });

  it('renders display section', () => {
    render(<IotPage />);
    expect(screen.getByText('Affichage — Dashboard')).toBeInTheDocument();
  });
});

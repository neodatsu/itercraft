import { render, screen } from '@testing-library/react';
import { HealthCheckPage } from './HealthCheckPage';

describe('HealthCheckPage', () => {
  it('renders UP status', () => {
    render(<HealthCheckPage />);
    expect(screen.getByText('UP')).toBeInTheDocument();
  });

  it('renders Health Check title', () => {
    render(<HealthCheckPage />);
    expect(screen.getByText('Health Check')).toBeInTheDocument();
  });
});

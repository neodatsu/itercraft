import { render, screen } from '@testing-library/react';
import { HealthCheckPage } from './HealthCheckPage';

describe('HealthCheckPage', () => {
  it('renders UP status', () => {
    render(<HealthCheckPage />);
    expect(screen.getByText('UP')).toBeInTheDocument();
  });

  it('renders title', () => {
    render(<HealthCheckPage />);
    expect(screen.getByText('État de santé')).toBeInTheDocument();
  });
});

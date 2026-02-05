import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StarRating } from './StarRating';

describe('StarRating', () => {
  it('renders 5 star buttons', () => {
    render(<StarRating value={null} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(5);
  });

  it('shows filled stars up to the value', () => {
    render(<StarRating value={3} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons[0]).toHaveClass('filled');
    expect(buttons[1]).toHaveClass('filled');
    expect(buttons[2]).toHaveClass('filled');
    expect(buttons[3]).not.toHaveClass('filled');
    expect(buttons[4]).not.toHaveClass('filled');
  });

  it('shows no filled stars when value is null', () => {
    render(<StarRating value={null} />);
    const buttons = screen.getAllByRole('button');
    buttons.forEach((button) => {
      expect(button).not.toHaveClass('filled');
    });
  });

  it('calls onChange when clicking a star', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<StarRating value={null} onChange={onChange} />);

    const buttons = screen.getAllByRole('button');
    await user.click(buttons[2]);

    expect(onChange).toHaveBeenCalledWith(3);
  });

  it('does not call onChange when readonly', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<StarRating value={3} onChange={onChange} readonly />);

    const buttons = screen.getAllByRole('button');
    await user.click(buttons[4]);

    expect(onChange).not.toHaveBeenCalled();
  });

  it('disables buttons when readonly', () => {
    render(<StarRating value={3} readonly />);
    const buttons = screen.getAllByRole('button');
    buttons.forEach((button) => {
      expect(button).toBeDisabled();
    });
  });

  it('has accessible labels for each star', () => {
    render(<StarRating value={null} />);
    expect(screen.getByRole('button', { name: '1 étoile' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2 étoiles' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '5 étoiles' })).toBeInTheDocument();
  });

  it('handles keyboard navigation', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<StarRating value={null} onChange={onChange} />);

    const buttons = screen.getAllByRole('button');
    buttons[2].focus();
    await user.keyboard('{Enter}');

    expect(onChange).toHaveBeenCalledWith(3);
  });
});

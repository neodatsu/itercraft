import './StarRating.css';

interface StarRatingProps {
  value: number | null;
  onChange?: (value: number) => void;
  readonly?: boolean;
  'aria-labelledby'?: string;
}

export function StarRating({ value, onChange, readonly = false, 'aria-labelledby': ariaLabelledBy }: Readonly<StarRatingProps>) {
  const handleClick = (starIndex: number) => {
    if (!readonly && onChange) {
      onChange(starIndex);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent, starIndex: number) => {
    if ((event.key === 'Enter' || event.key === ' ') && !readonly && onChange) {
      event.preventDefault();
      onChange(starIndex);
    }
  };

  return (
    <fieldset className={`star-rating ${readonly ? 'readonly' : ''}`} aria-labelledby={ariaLabelledBy}>
      <legend className="sr-only">{ariaLabelledBy ? 'Note' : 'Note du jeu'}</legend>
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          className={`star ${value !== null && star <= value ? 'filled' : ''}`}
          onClick={() => handleClick(star)}
          onKeyDown={(e) => handleKeyDown(e, star)}
          disabled={readonly}
          aria-label={`${star} étoile${star > 1 ? 's' : ''}`}
          aria-pressed={value !== null && star <= value}
        >
          ★
        </button>
      ))}
    </fieldset>
  );
}

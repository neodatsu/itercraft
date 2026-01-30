import './Footer.css';

export function Footer() {
  return (
    <footer className="app-footer">
      <span>&copy; {new Date().getFullYear()} Itercraft</span>
    </footer>
  );
}

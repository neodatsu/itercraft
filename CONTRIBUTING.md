# Contributing to Itercraft

Thank you for your interest in contributing to Itercraft! This document provides guidelines and instructions for contributing.

## Getting Started

### Prerequisites

- **Backend**: JDK 25, Maven
- **Frontend**: Node.js 25, npm
- **Infrastructure**: Docker, Terraform (optional)

### Local Development Setup

```bash
# Clone the repository
git clone https://github.com/neodatsu/itercraft.git
cd itercraft

# Backend
cd itercraft_api
mvn clean install
mvn spring-boot:run

# Frontend (in another terminal)
cd itercraft_front
npm ci
npm run dev
```

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](../../issues)
2. If not, create a new issue with:
   - Clear, descriptive title
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, browser, versions)

### Suggesting Features

Open an issue with the `enhancement` label describing:
- The problem you're trying to solve
- Your proposed solution
- Any alternatives you've considered

### Submitting Changes

1. **Fork** the repository
2. **Create a branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following our coding standards
4. **Test** your changes:
   ```bash
   # Backend
   cd itercraft_api && mvn verify

   # Frontend
   cd itercraft_front && npm test && npm run build
   ```
5. **Commit** with clear messages (in English)
6. **Push** and open a **Pull Request**

## Coding Standards

### Backend (Java/Spring Boot)

- Follow existing code style
- Add tests for new functionality
- Use SLF4J for logging with parameterized messages
- Validate user inputs at controller level
- Document public APIs

### Frontend (React/TypeScript)

- Use `Readonly<>` for component props
- Use `globalThis` instead of `window`
- Use `RegExp.exec()` instead of `string.match()`
- Use semantic HTML (`<section>` with `aria-label`)
- Use `React.SyntheticEvent<HTMLFormElement, SubmitEvent>` for form handlers

### General

- **Code**: English (variables, functions, classes)
- **UI/UX**: French
- **Commit messages**: English
- **Comments**: French or English (consistent within a file)

## Pull Request Guidelines

- Reference related issues (`Fixes #123`)
- Keep PRs focused on a single change
- Update documentation if needed
- Ensure CI passes (tests, linting, security checks)
- Request review from maintainers

## Development Workflow

```
main ─────────────────────────────────────────►
       \                    /
        feature/xyz ───────►
```

- All changes go through PRs to `main`
- PRs require passing CI checks
- Squash merge preferred for clean history

## Running Tests

```bash
# Backend unit tests
cd itercraft_api && mvn test

# Backend integration tests
cd itercraft_api && mvn verify

# Frontend tests
cd itercraft_front && npm test

# Frontend with coverage
cd itercraft_front && npm run test:coverage
```

## Code Review Process

1. Automated checks run (CI, SonarCloud, OWASP)
2. Maintainer reviews code
3. Address feedback
4. Merge when approved

## Questions?

- Open a [Discussion](../../discussions) for general questions
- Check [CLAUDE.md](CLAUDE.md) for detailed project conventions

## License

By contributing, you agree that your contributions will be licensed under the [GPL-3.0 License](LICENSE).

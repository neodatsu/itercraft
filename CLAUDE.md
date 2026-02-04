# Itercraft - Project Guidelines

## Project Structure

```
itercraft/
├── itercraft_api/      # Backend Java Spring Boot
├── itercraft_front/    # Frontend React TypeScript
└── .github/workflows/  # CI/CD GitHub Actions
```

## Backend (itercraft_api)

### Stack
- Java 25
- Spring Boot 4.0.2
- Maven
- JaCoCo for code coverage

### Conventions
- Use French for user-facing messages and API responses
- Follow standard Spring Boot project structure
- Tests in `src/test/java` mirroring `src/main/java` structure
- Use AssertJ for assertions (`assertThat`)
- Use MockRestServiceServer for mocking REST clients

### Commands
```bash
cd itercraft_api
mvn clean verify          # Build and run tests
mvn test                  # Run tests only
mvn dependency-check:check # OWASP dependency check
```

## Frontend (itercraft_front)

### Stack
- React 19
- TypeScript
- Vite
- Vitest for testing
- Testing Library for component tests

### Conventions
- Use French for UI labels and messages
- Use `vi.stubGlobal()` for mocking globals (not `global.X = ...`)
- Use `vi.mocked()` for typed mocks
- Prefer `useMemo` for Context provider values
- Use `localeCompare` for string sorting

### Commands
```bash
cd itercraft_front
npm ci                    # Install dependencies
npm test                  # Run tests
npm run build             # Production build
npx tsc --noEmit          # Type check
npx vitest run --coverage # Tests with coverage
```

## CI/CD

### Workflows
- `ci.yml` - Main CI pipeline (backend tests, frontend tests, SonarCloud, Lighthouse)
- `deploy.yml` - Deploy to ECR
- `terraform.yml` - Infrastructure deployment

### Quality Gates
- Code coverage target: 80%
- SonarCloud analysis required
- Lighthouse accessibility checks

## Code Style

### General
- Keep solutions simple and focused
- Avoid over-engineering
- No unnecessary abstractions
- Delete unused code completely

### Testing
- Write meaningful assertions
- Test behavior, not implementation
- Mock external dependencies
- Use descriptive test names

## Language

- Code: English (variables, functions, classes)
- UI/UX: French
- Comments: French or English (be consistent within a file)
- Commit messages: English

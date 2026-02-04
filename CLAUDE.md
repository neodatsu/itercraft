# Itercraft - Project Guidelines

## Project Structure

```
itercraft/
├── itercraft_api/      # Backend Java Spring Boot
├── itercraft_front/    # Frontend React TypeScript
└── .github/workflows/  # CI/CD GitHub Actions
```

## Architecture

### Domain-Driven Design (DDD)

The backend follows DDD principles with clear layer separation:

- **Domain Layer** (`domain/`): Entities, value objects, repository interfaces
- **Application Layer** (`application/`): Services (interface + implementation), use cases
- **Infrastructure Layer** (`infrastructure/`): REST controllers, security config, DTOs, external integrations

Rules:

- Domain layer has no dependencies on other layers
- Application layer depends only on domain
- Infrastructure layer can depend on all layers
- Keep business logic in domain/application, not in controllers

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

### Lint Rules (Prerequisites)

- **Readonly props**: Component props must be wrapped in `Readonly<>` (e.g., `{ children }: Readonly<{ children: ReactNode }>`)
- **globalThis over window**: Use `globalThis` instead of `window` (e.g., `globalThis.location.origin`)
- **RegExp.exec()**: Use `regex.exec(string)` instead of `string.match(regex)`
- **Semantic HTML**: Use `<section>` with `aria-label` instead of `<div role="region">`
- **Positive conditions**: Prefer `=== 1 ? '' : 's'` over `!== 1 ? 's' : ''`
- **Nullish coalescing assignment**: Use `??=` instead of `if (!x) { x = value }`
- **Form submit handler**: Use `React.SyntheticEvent<HTMLFormElement, SubmitEvent>` (FormEvent is deprecated)

### Commands

```bash
cd itercraft_front
npm ci                    # Install dependencies
npm test                  # Run tests
npm run build             # Production build
npx tsc --noEmit          # Type check
npx vitest run --coverage # Tests with coverage
```

## Regulatory Compliance

### GDPR & Privacy

- Personal data processing must be documented
- User consent required before collecting non-essential data
- Provide data export and deletion capabilities
- Privacy policy page required (`/confidentialite`)

### Cookies

- Cookie consent banner required before setting non-essential cookies
- Essential cookies only (session, CSRF) without consent
- Analytics (GA4) only after explicit user consent
- Cookie policy page required (`/cookies`)

### Legal

- Legal notices page required (`/mentions-legales`)
- Terms of service when applicable
- Clear identification of the service provider

## Accessibility

### Requirements

- Lighthouse accessibility score ≥ 90 (enforced in CI)
- WCAG 2.1 AA compliance target
- All images must have alt text
- Forms must have proper labels
- Color contrast ratios must be sufficient
- Keyboard navigation must work
- Screen reader compatibility

### Implementation

- Use semantic HTML elements (`<nav>`, `<main>`, `<article>`, etc.)
- Add `aria-label` attributes where needed
- Test with screen readers
- Ensure focus indicators are visible

## FinOps & Cost Management

### Before Adding Infrastructure

**ALWAYS ask before adding any new stack, service, or infrastructure component.**

Required information:

1. **Justification**: Why is this needed? Are there simpler alternatives?
2. **Cost estimate**: Monthly/yearly cost projection
3. **Alternatives considered**: Free or lower-cost options evaluated
4. **Resource sizing**: Start with smallest viable size

### Current Cost Targets

- AWS Budget alert: $10/month
- Prefer spot/preemptible instances when possible
- Use auto-scaling with conservative minimums
- Destroy non-production resources when not in use (`/infra destroy ec2`)

### Cost-Conscious Practices

- No over-provisioning (right-size resources)
- Use reserved capacity for predictable workloads
- Monitor costs regularly
- Clean up unused resources (old ECR images, snapshots, etc.)
- Prefer managed services only when cost-effective

## CI/CD

### Workflows

- `ci.yml` - Main CI pipeline (backend tests, frontend tests, SonarCloud, Lighthouse)
- `deploy.yml` - Deploy to ECR (only on `v*` tags)
- `terraform.yml` - Infrastructure deployment (manual trigger)

### Quality Gates

- Code coverage target: 80%
- SonarCloud analysis required
- Lighthouse accessibility score ≥ 90

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

## Documentation

### Keep Documentation in Sync

When making changes, update the following if affected:

- **README.md** - Project overview and quick start
- **Wiki** (`itercraft.wiki/`) - Detailed documentation
  - `Architecture-Overview.md` - System design changes
  - `API-Reference.md` - Endpoint changes
  - `Tutorial:-Your-First-Setup.md` - Setup process changes
- **Frontend pages** - If user-facing features change
  - Architecture page
  - Homepage

### Wiki Location

The wiki is a separate Git repository:

```bash
git clone https://github.com/neodatsu/itercraft.wiki.git
```

Changes to the wiki should follow the Diátaxis framework:

- **Tutorials** - Learning-oriented, step-by-step
- **How-to guides** - Task-oriented, practical
- **Reference** - Information-oriented, complete
- **Explanation** - Understanding-oriented, conceptual

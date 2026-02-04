# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| latest  | :white_check_mark: |

Only the latest version deployed in production receives security updates.

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please report it responsibly.

### How to Report

**Do NOT open a public GitHub issue for security vulnerabilities.**

Instead, please use one of the following methods:

1. **GitHub Security Advisories** (preferred): [Report a vulnerability](../../security/advisories/new)
2. **Email**: Send details to the repository maintainers

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### What to Expect

| Timeline | Action |
|----------|--------|
| 48 hours | Initial acknowledgment |
| 7 days   | Assessment and severity evaluation |
| 30 days  | Fix developed and tested |
| 45 days  | Public disclosure (coordinated) |

We may adjust timelines based on severity and complexity.

### Scope

In scope:
- itercraft_api (Spring Boot backend)
- itercraft_front (React frontend)
- Infrastructure configurations (Terraform, Docker)
- Authentication flows (Keycloak integration)

Out of scope:
- Third-party services (AWS, Cloudflare, Keycloak itself)
- Denial of Service attacks
- Social engineering

## Security Practices

This project implements:

- **Authentication**: OAuth 2.0 with Keycloak (opaque token introspection)
- **CSRF Protection**: Double Submit Cookie pattern
- **Input Validation**: Whitelist validation for user inputs
- **Dependency Scanning**: OWASP Dependency Check in CI/CD
- **Code Analysis**: SonarCloud static analysis
- **Secrets Management**: Environment variables, no hardcoded credentials

## Acknowledgments

We appreciate security researchers who help keep Itercraft secure. Contributors who report valid vulnerabilities will be acknowledged here (with permission).

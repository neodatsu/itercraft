# Itercraft

## Overview

Itercraft is a cloud-native web application deployed on AWS (eu-west-1), built with a Java/Spring Boot backend, a React/TypeScript frontend, and supported by a full DevSecOps pipeline.

## Architecture

```mermaid
graph TB
    subgraph CI/CD
        GH[GitHub Actions]
        GH -->|build & test| MVN[Maven + JaCoCo]
        GH -->|build & type check| VITE[Vite + TypeScript]
        GH -->|coverage report| PR[Pull Request]
        GH -->|dependency check| OWASP[OWASP]
        GH -->|code quality| SONAR[SonarCloud]
    end

    subgraph AWS
        R53[Route 53<br/>itercraft.com]
        ACM[ACM<br/>SSL Certificate]
        ECR[ECR<br/>itercraft_front<br/>itercraft_api]
        BUD[Budgets<br/>10$ alert]
        R53 --> ACM
    end

    subgraph Application
        API[itercraft_api<br/>Spring Boot 4 / Java 25<br/>:8080]
        FRONT[itercraft_front<br/>React / Vite / TypeScript<br/>:3000]
        KC[Keycloak 26<br/>OAuth2/OIDC + PKCE<br/>:8180]
        API -.-|/healthcheck| API
        FRONT -.-|/healthcheck| FRONT
        FRONT -->|auth| KC
    end

    subgraph Infrastructure
        TF[Terraform]
        DOCKER[Docker]
        TF --> AWS
        DOCKER --> API
        DOCKER --> FRONT
    end
```

## Project Structure

```
itercraft/
├── .github/workflows/     # CI/CD pipeline (backend + frontend)
├── devsecops/
│   ├── docker/
│   │   ├── Dockerfile          # Backend (multi-stage, Java 25)
│   │   ├── Dockerfile.front    # Frontend (multi-stage, Nginx)
│   │   ├── Dockerfile.keycloak # Keycloak (multi-stage, realm import)
│   │   └── keycloak/
│   │       └── itercraft-realm.json # Realm config (client, user, roles)
│   └── terraform/           # Infrastructure as Code
│       ├── aws_acm/         # SSL certificate (*.itercraft.com)
│       ├── aws_budget/      # Cost alert (10$/month)
│       ├── aws_ecr/         # Container registries (itercraft_api, itercraft_front)
│       ├── aws_route53/     # DNS (CNAME www + ACM validation)
│       ├── env.sh           # Environment variables (not committed)
│       └── tf.sh            # Terraform wrapper script
├── itercraft_api/           # Backend API (:8080)
│   └── src/
│       ├── main/            # Domain-Driven Design architecture
│       │   ├── domain/          # Value objects
│       │   ├── application/     # Services (interface + impl)
│       │   └── infrastructure/  # REST controllers
│       └── test/            # Unit & integration tests
└── itercraft_front/         # Frontend (:3000)
    └── src/
        ├── pages/           # Pages by feature
        ├── components/      # Reusable components
        └── utils/           # Utilities
```

## Tech Stack

| Layer          | Technology                         |
|----------------|------------------------------------|
| Backend        | Java 25, Spring Boot 4.0.2         |
| Frontend       | React, TypeScript, Vite            |
| Build          | Maven, JaCoCo, npm                 |
| Security       | OWASP Dependency-Check, SonarCloud |
| Auth           | Keycloak 26 (OAuth2/OIDC, PKCE)    |
| Infrastructure | Terraform, Docker, Nginx           |
| Cloud          | AWS (Route 53, ACM, ECR, Budgets)  |
| CI/CD          | GitHub Actions                     |
| Region         | eu-west-1 (Ireland)                |

## Getting Started

### Prerequisites

- Java 25
- Node.js 25
- Maven 3.8+
- Terraform 1.x
- Docker
- AWS CLI

### Run the API locally

```bash
cd itercraft_api
mvn spring-boot:run
```

The API is available at `http://localhost:8080/healthcheck`.

### Run the frontend locally

```bash
cd itercraft_front
npm install
npm run dev
```

The frontend is available at `http://localhost:3000/healthcheck`.

### Run tests

```bash
# Backend
cd itercraft_api
mvn clean verify

# Frontend
cd itercraft_front
npx tsc --noEmit
```

Backend coverage report is generated in `itercraft_api/target/site/jacoco/index.html`.

### Deploy infrastructure

```bash
cd devsecops/terraform
# Configure env.sh with your credentials

# 1. Budget (cost alert)
./tf.sh aws_budget init && ./tf.sh aws_budget apply

# 2. ACM (SSL certificate)
./tf.sh aws_acm init && ./tf.sh aws_acm apply

# 3. Route 53 (DNS + ACM validation) - depends on ACM
./tf.sh aws_route53 init && ./tf.sh aws_route53 apply

# 4. ECR (container registries)
./tf.sh aws_ecr init && ./tf.sh aws_ecr apply
```

### Build Docker images

```bash
# Backend
docker build -f devsecops/docker/Dockerfile -t itercraft-api .
docker run -p 8080:8080 itercraft-api

# Frontend
docker build -f devsecops/docker/Dockerfile.front -t itercraft-front .
docker run -p 3000:3000 itercraft-front

# Keycloak
docker build -f devsecops/docker/Dockerfile.keycloak -t itercraft-keycloak .

# Local (http://localhost:8180)
docker run -p 8180:8180 itercraft-keycloak

# Production (https://authent.itercraft.com)
docker run -p 8180:8180 -e KC_HOSTNAME=authent.itercraft.com itercraft-keycloak
```

## License

Proprietary - All rights reserved.

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
        GH -->|tag v*| ECR_PUSH[Push images to ECR]
    end

    subgraph AWS
        ECR[ECR<br/>6 repos]
        BUD[Budgets<br/>10$ alert]
        EC2[EC2 t3a.medium<br/>Ubuntu 22.04]
        EIP[Elastic IP]
        SG[Security Group<br/>Cloudflare IPs only]
        IAM[IAM Role<br/>ECR ReadOnly + SSM]
        EC2 --> EIP
        EC2 --> SG
        EC2 --> IAM
        IAM -->|pull images| ECR
    end

    subgraph Cloudflare
        CF[Cloudflare DNS<br/>itercraft.com]
        CF -->|proxy HTTPS| EIP
    end

    subgraph "Application (EC2 / docker-compose)"
        TRAEFIK[Traefik v3<br/>reverse proxy<br/>:80]
        TRAEFIK -->|www| FRONT[itercraft_front<br/>React / Vite / TypeScript]
        TRAEFIK -->|api| API[itercraft_api<br/>Spring Boot 4 / Java 25]
        TRAEFIK -->|authent| KC[Keycloak 26<br/>OAuth2/OIDC + PKCE<br/>:8180]
        TRAEFIK -->|grafana| GRAF[Grafana<br/>:3001]
        API -.-|/healthcheck| API
        FRONT -.-|/healthcheck| FRONT
        FRONT -->|auth| KC
        FRONT -->|API + CSRF| API
        API -->|token introspection| KC
        API -->|JPA| PG[PostgreSQL 17<br/>+ Liquibase<br/>:5432]
        PROM[Prometheus<br/>:9090] -->|scrape /actuator/prometheus| API
        GRAF -->|query| PROM
    end

    subgraph Infrastructure
        TF[Terraform]
        DOCKER[Docker]
        TF --> AWS
        DOCKER --> TRAEFIK
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
│   │   ├── Dockerfile.postgres # PostgreSQL 17 + Liquibase
│   │   ├── keycloak/
│   │   │   └── itercraft-realm.json # Realm config (clients, user, roles)
│   │   ├── prometheus/
│   │   │   └── prometheus.yml       # Scrape config (itercraft-api)
│   │   ├── grafana/
│   │   │   └── datasource.yml       # Prometheus datasource provisioning
│   │   ├── Dockerfile.prometheus    # Prometheus
│   │   ├── Dockerfile.grafana       # Grafana (port 3001)
│   │   └── postgres/
│   │       └── entrypoint-wrapper.sh # Postgres + Liquibase bootstrap
│   ├── liquibase/             # Database migrations
│   │   ├── db.changelog-master.yaml
│   │   └── changelogs/        # 001-init-schema, 002-seed-services
│   └── terraform/           # Infrastructure as Code
│       ├── aws_budget/      # Cost alert (10$/month)
│       ├── aws_ec2/         # EC2 + Elastic IP + SSM + Cloudflare DNS (Traefik + docker-compose)
│       ├── aws_ecr/         # Container registries (6 repos)
│       ├── aws_oidc_github/ # OIDC provider + IAM role (GitHub Actions → ECR)
│       ├── env.sh           # Environment variables (not committed)
│       └── tf.sh            # Terraform wrapper script
├── itercraft_api/           # Backend API (:8080)
│   └── src/
│       ├── main/            # Domain-Driven Design architecture
│       │   ├── domain/          # Entities, repositories, value objects
│       │   ├── application/     # Services (interface + impl)
│       │   └── infrastructure/  # REST controllers, security, DTOs
│       └── test/            # Unit & integration tests (H2)
└── itercraft_front/         # Frontend (:3000)
    └── src/
        ├── api/             # API client (subscriptions, CSRF)
        ├── auth/            # Keycloak auth provider + protected route
        ├── pages/           # Pages (home, dashboard, healthcheck)
        ├── components/      # Reusable components (Header, Footer)
        └── utils/           # Utilities
```

## Tech Stack

| Layer          | Technology                                            |
|----------------|-------------------------------------------------------|
| Backend        | Java 25, Spring Boot 4.0.2, Spring Security           |
| Frontend       | React, TypeScript, Vite                               |
| Database       | PostgreSQL 17, Liquibase (schema migrations)          |
| Build          | Maven, JaCoCo, npm, Vitest                            |
| Security       | OWASP Dependency-Check, SonarCloud, CSRF (cookie)     |
| Auth           | Keycloak 26 (OAuth2/OIDC, PKCE, token introspection) |
| Monitoring     | Prometheus, Grafana, Micrometer, Spring Boot Actuator |
| Infrastructure | Terraform, Docker, Nginx, Traefik                     |
| Cloud          | AWS (ECR, EC2, Elastic IP, Budgets, SSM), Cloudflare  |
| CI/CD          | GitHub Actions                                        |
| Region         | eu-west-1 (Ireland)                                   |

## Getting Started

### Prerequisites

- Java 25
- Node.js 25
- Maven 3.8+
- Terraform 1.x
- Docker
- AWS CLI

### Environment Variables

The backend is configured via environment variables (with defaults for local development):

| Variable                 | Default                 | Description                            |
|--------------------------|-------------------------|----------------------------------------|
| `DB_HOST`                | `localhost`             | PostgreSQL host                        |
| `DB_PORT`                | `5432`                  | PostgreSQL port                        |
| `DB_NAME`                | `itercraft`             | Database name                          |
| `DB_USER`                | `itercraft`             | Database user                          |
| `DB_PASSWORD`            | `itercraft`             | Database password                      |
| `KEYCLOAK_URL`           | `http://localhost:8180` | Keycloak base URL                      |
| `KEYCLOAK_REALM`         | `itercraft`             | Keycloak realm                         |
| `KEYCLOAK_CLIENT_ID`     | `iterapi`               | Confidential client for introspection  |
| `KEYCLOAK_CLIENT_SECRET` | `changeme`              | Client secret                          |
| `CORS_ORIGINS`           | `http://localhost:3000` | Allowed CORS origins (comma-separated) |

The frontend uses a `.env` file:

| Variable              | Default                  | Description           |
|-----------------------|--------------------------|-----------------------|
| `VITE_KEYCLOAK_URL`   | `http://localhost:8180`  | Keycloak base URL     |
| `VITE_KEYCLOAK_REALM` | `itercraft`              | Keycloak realm        |
| `VITE_API_URL`        | `http://localhost:8080`  | Backend API base URL  |

### Run the API locally

Requires a running PostgreSQL (with Liquibase migrations applied) and Keycloak instance.

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
# Backend (uses H2 in-memory database)
cd itercraft_api
mvn clean verify

# Frontend
cd itercraft_front
npx vitest run
```

Backend coverage report is generated in `itercraft_api/target/site/jacoco/index.html`.

### Deploy infrastructure

```bash
cd devsecops/terraform
# Configure env.sh with your credentials

# 1. Budget (cost alert)
./tf.sh aws_budget init && ./tf.sh aws_budget apply

# 2. ECR (container registries)
./tf.sh aws_ecr init && ./tf.sh aws_ecr apply

# 3. OIDC GitHub (IAM role for CI/CD → ECR push, no AWS keys needed)
./tf.sh aws_oidc_github init && ./tf.sh aws_oidc_github apply

# 4. EC2 + Cloudflare DNS (application server + Elastic IP + DNS records)
./tf.sh aws_ec2 init && ./tf.sh aws_ec2 apply
```

### Deploy images (CI/CD)

Le workflow `deploy.yml` est declenche automatiquement lors d'un tag `v*` :

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions build les 6 images Docker, les tag avec la version + `latest`, et les push sur ECR.

L'authentification utilise **OIDC** (OpenID Connect) : GitHub assume un role IAM directement aupres d'AWS, sans access keys stockees dans les secrets. Le module Terraform `aws_oidc_github` cree l'identity provider et le role `github-actions-ecr-push` restreint aux tags `v*` du repo.

Seul secret GitHub requis : `AWS_ACCOUNT_ID`.

### API Endpoints

| Method   | URL                                       | Auth          | Description                |
|----------|-------------------------------------------|---------------|----------------------------|
| `GET`    | `/healthcheck`                            | Public        | Health status              |
| `GET`    | `/actuator/health`                        | Public        | Actuator health            |
| `GET`    | `/actuator/prometheus`                    | Public        | Prometheus metrics         |
| `GET`    | `/api/subscriptions`                      | Bearer        | User subscriptions + usage |
| `GET`    | `/api/services`                           | Bearer        | All available services     |
| `POST`   | `/api/subscriptions/{serviceCode}`        | Bearer + CSRF | Subscribe to a service     |
| `DELETE` | `/api/subscriptions/{serviceCode}`        | Bearer + CSRF | Unsubscribe                |
| `POST`   | `/api/subscriptions/{serviceCode}/usages` | Bearer + CSRF | Add usage                  |
| `DELETE` | `/api/subscriptions/{serviceCode}/usages` | Bearer + CSRF | Remove usage               |

Mutation endpoints require an `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie.

### Build Docker images

```bash
docker build -f devsecops/docker/Dockerfile.postgres  -t itercraft-postgres .
docker build -f devsecops/docker/Dockerfile            -t itercraft-api .
docker build -f devsecops/docker/Dockerfile.front      -t itercraft-front .
docker build -f devsecops/docker/Dockerfile.keycloak    -t itercraft-keycloak .
docker build -f devsecops/docker/Dockerfile.prometheus  -t itercraft-prometheus .
docker build -f devsecops/docker/Dockerfile.grafana     -t itercraft-grafana .
```

### Run - Dev (containers on localhost)

Le backend accede aux autres services via `localhost` + port mapping Docker.
`KC_HOSTNAME=localhost` (defini dans le Dockerfile) fixe l'issuer Keycloak a `http://localhost:8180`.

```bash
# PostgreSQL
docker run -p 5432:5432 itercraft-postgres

# Keycloak
docker run -p 8180:8180 itercraft-keycloak

# Backend (sur le host, pas besoin de -e)
cd itercraft_api && mvn spring-boot:run

# Frontend
cd itercraft_front && npm run dev
```

### Run - Dev (tout en containers)

Les containers communiquent via le reseau Docker.
Le backend doit connaitre les IP ou hostnames Docker des autres services.

```bash
# Creer un reseau
docker network create itercraft

# PostgreSQL
docker run --network itercraft --name postgres -p 5432:5432 itercraft-postgres

# Keycloak (KC_HOSTNAME=localhost pour que l'issuer matche le navigateur)
docker run --network itercraft --name keycloak -p 8180:8180 itercraft-keycloak

# Backend (DB_HOST et KEYCLOAK_URL pointent vers les hostnames Docker)
docker run --network itercraft --name api -p 8080:8080 \
  -e DB_HOST=postgres \
  -e KEYCLOAK_URL=http://keycloak:8180 \
  -e CORS_ORIGINS=http://localhost:3000 \
  itercraft-api

# Frontend
docker run --network itercraft --name front -p 3000:3000 itercraft-front

# Prometheus
docker run --network itercraft --name prometheus -p 9090:9090 itercraft-prometheus

# Grafana (admin/admin)
docker run --network itercraft --name grafana -p 3001:3001 itercraft-grafana
```

### Run - Production

```bash
docker run --network itercraft --name postgres \
  -e POSTGRES_PASSWORD=<secret> \
  itercraft-postgres

docker run --network itercraft --name keycloak \
  -e KC_HOSTNAME=authent.itercraft.com \
  -e KC_HOSTNAME_PORT=-1 \
  itercraft-keycloak

docker run --network itercraft --name api \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=<secret> \
  -e KEYCLOAK_URL=http://keycloak:8180 \
  -e KEYCLOAK_CLIENT_SECRET=<secret> \
  -e CORS_ORIGINS=https://www.itercraft.com \
  itercraft-api

docker run --network itercraft --name front -p 3000:3000 itercraft-front

docker run --network itercraft --name prometheus -p 9090:9090 itercraft-prometheus

docker run --network itercraft --name grafana -p 3001:3001 itercraft-grafana
```

## License

Proprietary - All rights reserved.

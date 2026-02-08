# Itercraft

## Overview

Itercraft is a cloud-native web application deployed on AWS (eu-west-1), built with a Java/Spring Boot backend, a React/TypeScript frontend, and supported by a full DevSecOps pipeline.

## Features

- **Subscriptions** — Manage service subscriptions with usage tracking
- **Entretien** — Track home maintenance activities with time logging and auto-stop
- **Weather Activities** — AI-powered activity suggestions based on weather analysis (Claude + Météo France)
- **Ma Ludothèque** — Personal board game collection with AI-powered game info and suggestions
- **IoT Sensors** — MQTT-based sensor data collection (ESP32, TLS 1.3)
- **Observability** — Full monitoring stack (Prometheus, Loki, Tempo, Grafana)
- **Runtime Security** — Threat detection with Falco (eBPF syscall monitoring)

## Architecture

```mermaid
graph TB
    subgraph CI/CD
        GH[GitHub Actions]
        GH -->|build & test| MVN[Maven + JaCoCo]
        GH -->|build & type check| VITE[Vite + TypeScript]
        GH -->|coverage report| PR[Pull Request]
        GH -->|dependency check| OWASP[OWASP]
        GH -->|SBOM + vuln scan| TRIVY[Trivy + CycloneDX]
        GH -->|code quality| SONAR[SonarCloud]
        GH -->|accessibility| LH[Lighthouse CI]
        GH -->|tag v*| ECR_PUSH[Push images to ECR]
        GH -->|notify| SLACK[Slack]
        SLACK -->|/infra apply| GH
        SLACK -->|/infra destroy| GH
    end

    subgraph AWS
        ECR[ECR<br/>12 repos]
        BUD[Budgets<br/>10$ alert]
        EC2[EC2 t3.large<br/>Ubuntu 22.04]
        EIP[Elastic IP]
        SG[Security Group<br/>Cloudflare IPs only]
        IAM[IAM Role<br/>ECR ReadOnly + SSM]
        S3[S3<br/>Terraform state]
        DDB[DynamoDB<br/>State locking]
        LAMBDA[Lambda<br/>Slack bridge]
        APIGW[API Gateway<br/>Slack webhook]
        EC2 --> EIP
        EC2 --> SG
        EC2 --> IAM
        IAM -->|pull images| ECR
        APIGW --> LAMBDA
        LAMBDA -->|workflow_dispatch| GH
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
        FRONT -->|SSE /api/events| API
        API -->|JWT validation| KC
        API -->|JPA| PG[PostgreSQL 17<br/>+ Liquibase<br/>:5432]
        API -->|suggest activities| CLAUDE[Claude API<br/>Anthropic]
        API -->|WMS GetMap| MF[Météo France<br/>AROME PI]
        PROM[Prometheus<br/>:9090] -->|scrape /actuator/prometheus| API
        GRAF -->|query| PROM
        LOKI[Loki<br/>:3100] -->|store| LOGS[(Logs)]
        PROMTAIL[Promtail] -->|push logs| LOKI
        PROMTAIL -->|scrape| API
        PROMTAIL -->|scrape| KC
        TEMPO[Tempo<br/>:3200] -->|store| TRACES[(Traces)]
        API -->|send traces| TEMPO
        GRAF -->|query logs| LOKI
        GRAF -->|query traces| TEMPO
        MQTT[Mosquitto<br/>MQTT Broker<br/>:8883 TLS]
        API -->|subscribe sensors/#| MQTT
    end

    subgraph IoT
        ESP32[ESP32<br/>Capteurs]
        ESP32 -->|MQTTS 8883| MQTT
    end

    subgraph Infrastructure
        TF[Terraform]
        DOCKER[Docker]
        TF --> AWS
        DOCKER --> TRAEFIK
    end
```

## Domain Model — Ludothèque

```mermaid
classDiagram
    direction LR

    class Jeu {
        UUID id
        String nom
        String description
        Short joueursMin
        Short joueursMax
        Short dureeMoyenneMinutes
        String imageUrl
    }

    class JeuUser {
        UUID id
        String userSub
        Short note
        LocalDateTime createdAt
    }

    class TypeJeu {
        UUID id
        String code
        String libelle
    }

    class AgeJeu {
        UUID id
        String code
        String libelle
        Short ageMinimum
    }

    class ComplexiteJeu {
        UUID id
        Short niveau
        String libelle
    }

    Jeu "*" -- "1" TypeJeu : type
    Jeu "*" -- "1" AgeJeu : âge
    Jeu "*" -- "1" ComplexiteJeu : complexité
    JeuUser "*" -- "1" Jeu : jeu
```

**Reference data:**

- **TypeJeu**: lettres_mots, strategie, enquete_escape, culture_quiz, ambiance, classique, reflexion, adresse
- **AgeJeu**: enfant, tout_public, adulte
- **ComplexiteJeu**: 1 (Très simple), 2 (Simple), 3 (Moyen), 4 (Complexe), 5 (Expert)

## Domain Model — Maintenance Sessions

```mermaid
classDiagram
    direction LR

    class MaintenanceSession {
        UUID id
        OffsetDateTime startedAt
        OffsetDateTime endedAt
        Integer durationMinutes
        Boolean autoStopped
    }

    class AppUser {
        UUID id
        String keycloakSub
    }

    class ServiceEntity {
        UUID id
        String code
        String label
    }

    MaintenanceSession "*" -- "1" AppUser : user
    MaintenanceSession "*" -- "1" ServiceEntity : service
```

**Features:**

- Time tracking with start/stop buttons
- Automatic stop after 4 hours
- Duration aggregation by day/week/month/year
- SSE real-time updates

## Domain Model — IoT Sensors

```mermaid
classDiagram
    direction LR

    class SensorDevice {
        UUID id
        String name
        OffsetDateTime createdAt
    }

    class SensorData {
        UUID id
        OffsetDateTime measuredAt
        OffsetDateTime receivedAt
        Double dhtTemperature
        Double dhtHumidity
        Double ntcTemperature
        Double luminosity
    }

    class AppUser {
        UUID id
        String keycloakSub
        String email
    }

    SensorDevice "*" -- "1" AppUser : user
    SensorData "*" -- "1" SensorDevice : device
```

**Data flow:** ESP32 → MQTTS 8883 → Mosquitto → Spring Integration → `SensorDataService` → PostgreSQL → REST API → Dashboard (recharts)

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
│   │   │   ├── datasource.yml       # Prometheus, Loki, Tempo datasources
│   │   │   └── dashboards/          # Pre-configured dashboards
│   │   ├── loki/
│   │   │   └── loki-config.yml      # Loki configuration (log storage)
│   │   ├── promtail/
│   │   │   └── promtail-config.yml  # Promtail configuration (log collection)
│   │   ├── tempo/
│   │   │   └── tempo-config.yml     # Tempo configuration (trace storage)
│   │   ├── Dockerfile.prometheus    # Prometheus
│   │   ├── Dockerfile.grafana       # Grafana (port 3001)
│   │   ├── Dockerfile.loki          # Loki (log aggregation)
│   │   ├── Dockerfile.promtail      # Promtail (log collector)
│   │   ├── Dockerfile.tempo         # Tempo (distributed tracing)
│   │   └── postgres/
│   │       └── entrypoint-wrapper.sh # Postgres + Liquibase bootstrap
│   ├── liquibase/             # Database migrations
│   │   ├── db.changelog-master.yaml
│   │   └── changelogs/        # 001-init-schema, 002-seed-services
│   └── terraform/           # Infrastructure as Code
│       ├── aws_budget/      # Cost alert (10$/month)
│       ├── aws_ec2/         # EC2 + Elastic IP + SSM + Cloudflare DNS (Traefik + docker-compose)
│       ├── aws_ecr/         # Container registries (12 repos)
│       ├── aws_oidc_github/ # OIDC provider + IAM roles (GitHub Actions → ECR + Terraform)
│       ├── aws_backend/     # S3 bucket + DynamoDB for Terraform state (remote backend)
│       ├── aws_lambda_slack/# Lambda + API Gateway for Slack /infra command
│       ├── env.sh           # Environment variables (not committed)
│       └── tf.sh            # Terraform wrapper script
├── iot/                     # IoT / MQTT
│   └── mosquitto/           # MQTT broker configuration
│       ├── Dockerfile           # Mosquitto 2.0 with TLS
│       ├── mosquitto.conf       # Broker config (TLS 1.3, auth, ACL)
│       ├── acl.conf             # Topic-based access control
│       ├── docker-compose.yml   # Local development
│       └── scripts/             # Certificate & user management
│           ├── generate-certs.sh
│           ├── add-user.sh
│           └── generate-device-cert.sh
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

| Layer          | Technology                                                                              |
|----------------|-----------------------------------------------------------------------------------------|
| Backend        | Java 25, Spring Boot 4.0.2, Spring Security                                             |
| Frontend       | React, TypeScript, Vite                                                                 |
| Database       | PostgreSQL 17, Liquibase (schema migrations)                                            |
| Build          | Maven, JaCoCo, npm, Vitest                                                              |
| Analytics      | Google Analytics (GA4, after consent only)                                              |
| Accessibility  | Lighthouse CI (score ≥ 90 in CI)                                                        |
| Security       | OWASP Dependency-Check, Trivy, SBOM (CycloneDX), SonarCloud, CSRF (cookie), Falco (runtime) |
| Auth           | Keycloak 26 (OAuth2/OIDC, PKCE, JWT)                                                    |
| AI / Vision    | Claude API, Anthropic (activity suggestions based on weather)                           |
| Real-time      | Server-Sent Events (SSE, SseEmitter)                                                    |
| IoT            | Mosquitto MQTT (TLS 1.3, password auth, ACL), ESP32                                     |
| Monitoring     | Prometheus, Grafana, Loki (logs), Tempo (traces), Micrometer, Spring Boot Actuator      |
| Resilience     | Resilience4j (Circuit Breaker, Retry, Time Limiter)                                     |
| Infrastructure | Terraform, Docker, Nginx, Traefik                                                       |
| Cloud          | AWS (ECR, EC2, Elastic IP, Budgets, SSM, S3, DynamoDB, Lambda, API Gateway), Cloudflare |
| CI/CD          | GitHub Actions, Slack ChatOps (`/infra`)                                                |
| Region         | eu-west-1 (Ireland)                                                                     |

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

| Variable                 | Default                                    | Description                            |
|--------------------------|--------------------------------------------|----------------------------------------|
| `DB_HOST`                | `localhost`                                | PostgreSQL host                        |
| `DB_PORT`                | `5432`                                     | PostgreSQL port                        |
| `DB_NAME`                | `itercraft`                                | Database name                          |
| `DB_USER`                | `itercraft`                                | Database user                          |
| `DB_PASSWORD`            | `itercraft`                                | Database password                      |
| `KEYCLOAK_INTERNAL_URL`  | `http://keycloak:8180`                     | Keycloak internal URL (for JWK fetch)  |
| `KEYCLOAK_ISSUER_URI`    | `http://localhost:8180/realms/itercraft`   | Expected JWT issuer (external URL)     |
| `KEYCLOAK_REALM`         | `itercraft`                                | Keycloak realm                         |
| `CORS_ORIGINS`           | `http://localhost:3000`                    | Allowed CORS origins (comma-separated) |
| `METEOFRANCE_API_TOKEN`  | `changeme`                                 | Météo France API key                   |
| `ANTHROPIC_API_KEY`      | `changeme`                                 | Anthropic API key (Claude)             |
| `ANTHROPIC_MODEL`        | `claude-sonnet-4-20250514`                 | Claude model for image analysis        |
| `MQTT_HOST`              | `localhost`                                | MQTT broker host                       |
| `MQTT_PORT`              | `8883`                                     | MQTT broker port (TLS)                 |
| `MQTT_BACKEND_USER`      | `itercraft-backend`                        | MQTT service account username          |
| `MQTT_BACKEND_PASSWORD`  | `changeme`                                 | MQTT service account password          |
| `MQTT_TRUST_ALL_CERTS`   | `false`                                    | Trust self-signed certs (dev only)     |

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

#### Required credentials

1. **AWS**: Configure `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and `AWS_MFA_SERIAL` in `devsecops/terraform/env.sh`
2. **Cloudflare API Token**:
   - Cloudflare Dashboard → My Profile → API Tokens → Create Token
   - Template: **Edit zone DNS**
   - Add permission: Zone → Zone Settings → Edit
   - Zone Resources: Include → itercraft.com
   - Copy the token to `TF_VAR_cloudflare_api_token` in `env.sh`
3. **GitHub Secrets** (for CI/CD): `AWS_ACCOUNT_ID` in the `itercraft` environment secrets

#### Terraform apply

```bash
cd devsecops/terraform
# Configure env.sh with your credentials

# 1. Budget (cost alert)
./tf.sh aws_budget init && ./tf.sh aws_budget apply

# 2. ECR (container registries)
./tf.sh aws_ecr init && ./tf.sh aws_ecr apply

# 3. OIDC GitHub (IAM role for CI/CD → ECR push, no AWS keys needed)
./tf.sh aws_oidc_github init && ./tf.sh aws_oidc_github apply

# 4. EC2 + Cloudflare DNS (Elastic IP + DNS records + SSL Flexible)
./tf.sh aws_ec2 init && ./tf.sh aws_ec2 apply
```

### Deploy images (CI/CD)

The `deploy.yml` workflow is automatically triggered on `v*` tags:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions builds the 12 Docker images, tags them with the version + `latest`, and pushes them to ECR.

Authentication uses **OIDC** (OpenID Connect): GitHub assumes an IAM role directly with AWS, without access keys stored in secrets. The `aws_oidc_github` Terraform module creates the identity provider and the `github-actions-ecr-push` role restricted to `v*` tags of the repo.

Only required GitHub secret: `AWS_ACCOUNT_ID`.

### Deploy infrastructure via Slack (ChatOps)

The EC2 stack can be deployed or destroyed directly from Slack using the `/infra` command:

```text
/infra apply ec2    # Terraform apply on aws_ec2
/infra destroy ec2  # Terraform destroy on aws_ec2
/infra plan ec2     # Terraform plan (dry-run)
```

The workflow is triggered via GitHub Actions (`terraform.yml`) and notifies the result in Slack.

#### Architecture ChatOps

```text
Slack /infra command
       ↓
API Gateway (HTTPS)
       ↓
Lambda (Node.js) — verifies Slack signature, calls GitHub API
       ↓
GitHub Actions workflow_dispatch
       ↓
Terraform apply/destroy (OIDC → AWS)
       ↓
Slack notification (webhook)
```

#### Setup ChatOps

1. **Backend S3 + DynamoDB** (remote state + locking):

   ```bash
   ./tf.sh aws_backend init
   ./tf.sh aws_backend apply
   ```

   Creates the `itercraft-terraform-state` bucket and the `itercraft-terraform-locks` table.

2. **Terraform OIDC Role**:

   ```bash
   ./tf.sh aws_oidc_github init
   ./tf.sh aws_oidc_github apply
   ```

   Adds the `github-actions-terraform` role with EC2/S3/DynamoDB permissions.

3. **GitHub Secrets** (Settings → Environments → `itercraft`):
   - `AWS_TERRAFORM_ROLE_ARN`: ARN of the terraform role (output from aws_oidc_github)
   - `SLACK_WEBHOOK_URL`: Slack Incoming Webhook for notifications

4. **Slack App**:
   - Create an app at <https://api.slack.com/apps>
   - Add a Slash Command `/infra`
   - Note the **Signing Secret** (Basic Information → App Credentials)

5. **Lambda Slack → GitHub**:

   ```bash
   ./tf.sh aws_lambda_slack init
   ./tf.sh aws_lambda_slack apply
   ```

   Required variables (via `terraform.tfvars` or `-var`):
   - `github_token`: Personal Access Token (classic) with `repo` scope
   - `slack_signing_secret`: Signing Secret from the Slack App

   Configure the `slack_webhook_url` output as the Request URL for the `/infra` slash command.

6. **Initialize EC2 with S3 backend** (first time only):
   ```bash
   cd devsecops/terraform/aws_ec2
   source ../env.sh
   terraform init \
     -backend-config="bucket=itercraft-terraform-state" \
     -backend-config="key=aws_ec2/terraform.tfstate" \
     -backend-config="region=eu-west-1" \
     -backend-config="dynamodb_table=itercraft-terraform-locks" \
     -backend-config="encrypt=true"
   ```

Then, use `/infra apply ec2` or `/infra destroy ec2` from Slack.

### API Endpoints

| Method   | URL                                       | Auth          | Description                |
|----------|-------------------------------------------|---------------|----------------------------|
| `GET`    | `/healthcheck`                            | Public        | Health status              |
| `GET`    | `/actuator/health`                        | Public        | Actuator health            |
| `GET`    | `/actuator/prometheus`                    | Public        | Prometheus metrics         |
| `GET`    | `/api/events`                             | Public        | SSE stream (real-time)     |
| `GET`    | `/api/resilience/status`                  | Public        | Circuit breaker metrics    |
| `GET`    | `/api/resilience/health`                  | Public        | Resilience health status   |
| `GET`    | `/api/subscriptions`                      | Bearer        | User subscriptions + usage |
| `GET`    | `/api/services`                           | Bearer        | All available services     |
| `POST`   | `/api/subscriptions/{serviceCode}`        | Bearer + CSRF | Subscribe to a service     |
| `DELETE` | `/api/subscriptions/{serviceCode}`        | Bearer + CSRF | Unsubscribe                |
| `POST`   | `/api/subscriptions/{serviceCode}/usages` | Bearer + CSRF | Add usage                  |
| `DELETE` | `/api/subscriptions/{serviceCode}/usages` | Bearer + CSRF | Remove usage               |
| `POST`   | `/api/activities/suggest`                 | Bearer + CSRF | AI activity suggestions (JSON) |
| `GET`    | `/api/ludotheque/jeux`                    | Bearer        | Game catalog               |
| `GET`    | `/api/ludotheque/mes-jeux`                | Bearer        | User's game collection     |
| `GET`    | `/api/ludotheque/references`              | Bearer        | Reference tables           |
| `POST`   | `/api/ludotheque/mes-jeux`                | Bearer + CSRF | Add game by title (AI)     |
| `POST`   | `/api/ludotheque/mes-jeux/{jeuId}`        | Bearer + CSRF | Add game to collection     |
| `DELETE` | `/api/ludotheque/mes-jeux/{jeuId}`        | Bearer + CSRF | Remove game                |
| `PUT`    | `/api/ludotheque/mes-jeux/{jeuId}/note`   | Bearer + CSRF | Update rating (1-5 stars)  |
| `POST`   | `/api/ludotheque/suggestion`              | Bearer + CSRF | AI game suggestion         |
| `GET`    | `/api/maintenance/activities`                | Bearer        | Maintenance activities     |
| `POST`   | `/api/maintenance/activities`                | Bearer + CSRF | Create new activity        |
| `POST`   | `/api/maintenance/activities/{code}/start`   | Bearer + CSRF | Start activity timer       |
| `POST`   | `/api/maintenance/activities/{code}/stop`    | Bearer + CSRF | Stop activity timer        |
| `GET`    | `/api/maintenance/totals`                    | Bearer        | Aggregated time totals     |
| `GET`    | `/api/maintenance/activities/{code}/history` | Bearer        | Activity session history   |
| `DELETE` | `/api/maintenance/sessions/{id}`             | Bearer + CSRF | Delete a session           |
| `GET`    | `/api/sensors/data?from=...&to=...`          | Bearer        | Sensor data (default: 7 days) |

Mutation endpoints require an `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie.

### Resilience Patterns (Circuit Breaker)

The application uses **Resilience4j** to protect against cascading failures when external services (Météo France, Claude) become slow or unavailable.

#### Circuit Breaker States

```text
CLOSED → (failures > threshold) → OPEN → (wait duration) → HALF_OPEN → (success) → CLOSED
                                    ↑                           │
                                    └───── (failure) ───────────┘
```

- **CLOSED**: Normal operation, requests pass through
- **OPEN**: Circuit tripped, requests fail fast with fallback
- **HALF_OPEN**: Testing recovery, limited requests allowed

#### Configuration

| Service       | Window | Failure Threshold | Wait Duration | Timeout |
|---------------|--------|-------------------|---------------|---------|
| Météo France  | 10     | 50%               | 30s           | 10s     |
| Claude        | 5      | 50%               | 60s           | 60s     |

Météo France also has automatic retry (3 attempts with exponential backoff).

#### Endpoints

- `GET /api/resilience/status` - Detailed circuit breaker metrics
- `GET /api/resilience/health` - Overall health (UP, DEGRADED, RECOVERING)
- `GET /actuator/circuitbreakers` - Native Actuator endpoint

#### Frontend Page

A public page explaining the resilience patterns is available at `/resilience`, showing:

- Circuit breaker concept diagrams
- Real-time status of all circuit breakers
- Configuration details

### Build Docker images

```bash
docker build -f devsecops/docker/Dockerfile.postgres  -t itercraft-postgres .
docker build -f devsecops/docker/Dockerfile            -t itercraft-api .
docker build -f devsecops/docker/Dockerfile.front      -t itercraft-front .
docker build -f devsecops/docker/Dockerfile.keycloak    -t itercraft-keycloak .
docker build -f devsecops/docker/Dockerfile.prometheus  -t itercraft-prometheus .
docker build -f devsecops/docker/Dockerfile.grafana     -t itercraft-grafana .
docker build -f devsecops/docker/Dockerfile.loki        -t itercraft-loki .
docker build -f devsecops/docker/Dockerfile.promtail    -t itercraft-promtail .
docker build -f devsecops/docker/Dockerfile.tempo       -t itercraft-tempo .
docker build -f devsecops/docker/Dockerfile.falco       -t itercraft-falco .
docker build -f devsecops/docker/Dockerfile.falcosidekick -t itercraft-falcosidekick .
```

### Run - Dev (containers on localhost)

The backend accesses other services via `localhost` + Docker port mapping.
`KC_HOSTNAME=localhost` (defined in the Dockerfile) sets the Keycloak issuer to `http://localhost:8180`.

```bash
# PostgreSQL
docker run -p 5432:5432 itercraft-postgres

# Keycloak
docker run -p 8180:8180 itercraft-keycloak

# Backend (on host, no -e needed)
cd itercraft_api && mvn spring-boot:run

# Frontend
cd itercraft_front && npm run dev
```

### Run - Dev (all in containers)

Containers communicate via the Docker network.
The backend must know the Docker IP addresses or hostnames of other services.

```bash
# Create a network
docker network create itercraft

# PostgreSQL
docker run --network itercraft --name postgres -p 5432:5432 itercraft-postgres

# Keycloak (KC_HOSTNAME=localhost so the issuer matches the browser)
docker run --network itercraft --name keycloak -p 8180:8180 itercraft-keycloak

# Backend (DB_HOST and KEYCLOAK_INTERNAL_URL point to Docker hostnames)
docker run --network itercraft --name api -p 8080:8080 \
  -e DB_HOST=postgres \
  -e KEYCLOAK_INTERNAL_URL=http://keycloak:8180 \
  -e CORS_ORIGINS=http://localhost:3000 \
  -e ANTHROPIC_API_KEY=<your-key> \
  itercraft-api

# Frontend
docker run --network itercraft --name front -p 3000:3000 itercraft-front

# Prometheus
docker run --network itercraft --name prometheus -p 9090:9090 itercraft-prometheus

# Loki (log aggregation)
docker run --network itercraft --name loki -p 3100:3100 itercraft-loki

# Tempo (distributed tracing)
docker run --network itercraft --name tempo -p 3200:3200 -p 4317:4317 -p 4318:4318 itercraft-tempo

# Promtail (log collector - needs Docker socket access)
docker run --network itercraft --name promtail \
  -v /var/lib/docker/containers:/var/lib/docker/containers:ro \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  itercraft-promtail

# Falcosidekick (alert forwarder - start before Falco)
docker run --network itercraft --name falcosidekick -p 2801:2801 \
  -e SLACK_WEBHOOK_URL=<your-slack-webhook> \
  itercraft-falcosidekick

# Falco (runtime security - needs privileged mode for eBPF)
docker run --network itercraft --name falco --privileged \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /dev:/host/dev:ro \
  -v /proc:/host/proc:ro \
  -v /etc:/host/etc:ro \
  itercraft-falco

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
  -e KEYCLOAK_INTERNAL_URL=http://keycloak:8180 \
  -e KEYCLOAK_ISSUER_URI=https://authent.itercraft.com/realms/itercraft \
  -e CORS_ORIGINS=https://www.itercraft.com \
  itercraft-api

docker run --network itercraft --name front -p 3000:3000 itercraft-front

docker run --network itercraft --name prometheus -p 9090:9090 itercraft-prometheus

docker run --network itercraft --name loki itercraft-loki

docker run --network itercraft --name tempo itercraft-tempo

docker run --network itercraft --name promtail \
  -v /var/lib/docker/containers:/var/lib/docker/containers:ro \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  itercraft-promtail

docker run --network itercraft --name falcosidekick \
  -e SLACK_WEBHOOK_URL=<your-slack-webhook> \
  itercraft-falcosidekick

docker run --network itercraft --name falco --privileged \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /dev:/host/dev:ro \
  -v /proc:/host/proc:ro \
  -v /etc:/host/etc:ro \
  itercraft-falco

docker run --network itercraft --name grafana -p 3001:3001 itercraft-grafana
```

## IoT / MQTT Setup

Itercraft includes a secure MQTT broker (Mosquitto) for IoT device connectivity.

### Security Features

- **TLS 1.3** : All connections encrypted with modern cipher suites
- **Password Authentication** : No anonymous connections allowed
- **ACL** : Topic-based access control (devices can only publish to their own topics)
- **DNS-only** : `mqtt.itercraft.com` points directly to EC2 (no Cloudflare proxy for TCP)

### Broker Initial Setup (one-time)

```bash
cd iot/mosquitto

# 1. Generate CA and server certificates
./scripts/generate-certs.sh

# 2. Create admin user
./scripts/add-user.sh admin
# Enter a strong password when prompted

# 3. Create backend service account
./scripts/add-user.sh itercraft-backend
# Enter a strong password when prompted
```

Output files:

- `certs/ca.crt` : CA certificate (to distribute to IoT devices)
- `certs/server.crt` / `server.key` : Server TLS certificate
- `passwd` : Password file (hashed, safe to commit)

### Device Enrollment Process

#### Step 1: Generate device credentials

```bash
cd iot/mosquitto

# Create MQTT user for the device
# Convention: device-<type>-<serial>
./scripts/add-user.sh device-esp32-ABC123 "$(openssl rand -base64 24)"
```

Save the generated password securely (e.g., password manager, secrets vault).

#### Step 2: Prepare device provisioning files

Create a provisioning folder for the device:

```bash
DEVICE_ID="esp32-ABC123"
mkdir -p provisioning/$DEVICE_ID

# Copy CA certificate
cp certs/ca.crt provisioning/$DEVICE_ID/

# Create credentials file
cat > provisioning/$DEVICE_ID/mqtt_credentials.h << EOF
#ifndef MQTT_CREDENTIALS_H
#define MQTT_CREDENTIALS_H

#define MQTT_BROKER     "mqtt.itercraft.com"
#define MQTT_PORT       8883
#define MQTT_USER       "device-$DEVICE_ID"
#define MQTT_PASSWORD   "<password-from-step-1>"
#define DEVICE_ID       "$DEVICE_ID"

// CA Certificate (copy content of ca.crt)
const char* ca_cert = R"EOF(
-----BEGIN CERTIFICATE-----
<PASTE CONTENT OF ca.crt HERE>
-----END CERTIFICATE-----
)EOF";

#endif
EOF
```

#### Step 3: Flash the device

1. Open the ESP32 project in PlatformIO/Arduino IDE
2. Copy `mqtt_credentials.h` to the `src/` folder
3. Build and upload to the device
4. Monitor Serial output to verify connection

#### Step 4: Verify enrollment

```bash
# Subscribe to device topics (from server or local with mosquitto-clients)
mosquitto_sub -h mqtt.itercraft.com -p 8883 \
  --cafile certs/ca.crt \
  -u admin -P <admin-password> \
  -t "sensors/esp32-ABC123/#" -v

# Expected output when device publishes:
# sensors/esp32-ABC123/temperature 23.5
# sensors/esp32-ABC123/humidity 65.2
```

### Topic Structure & ACL

| Topic Pattern | Access | Description |
|---------------|--------|-------------|
| `sensors/<device_id>/#` | Device: write | Sensor readings (temperature, humidity, etc.) |
| `devices/<device_id>/status` | Device: write | Heartbeat, battery level, WiFi RSSI |
| `commands/<device_id>/#` | Device: read | Commands from backend (reboot, config update) |
| `broadcast/#` | All: read | Global announcements (maintenance, updates) |

**ACL enforcement**: A device `device-esp32-ABC123` can only:

- Publish to `sensors/esp32-ABC123/*` and `devices/esp32-ABC123/*`
- Subscribe to `commands/esp32-ABC123/*` and `broadcast/*`

### ESP32 Reference Implementation

```cpp
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include "mqtt_credentials.h"  // Generated during enrollment

WiFiClientSecure espClient;
PubSubClient mqtt(espClient);

void connectMQTT() {
  espClient.setCACert(ca_cert);
  mqtt.setServer(MQTT_BROKER, MQTT_PORT);
  mqtt.setCallback(onMessage);

  while (!mqtt.connected()) {
    Serial.print("MQTT connecting...");
    if (mqtt.connect(DEVICE_ID, MQTT_USER, MQTT_PASSWORD)) {
      Serial.println("connected");

      // Subscribe to commands
      char cmdTopic[64];
      snprintf(cmdTopic, sizeof(cmdTopic), "commands/%s/#", DEVICE_ID);
      mqtt.subscribe(cmdTopic);
      mqtt.subscribe("broadcast/#");

      // Publish online status
      char statusTopic[64];
      snprintf(statusTopic, sizeof(statusTopic), "devices/%s/status", DEVICE_ID);
      mqtt.publish(statusTopic, "{\"status\":\"online\"}");
    } else {
      Serial.printf("failed, rc=%d\n", mqtt.state());
      delay(5000);
    }
  }
}

void onMessage(char* topic, byte* payload, unsigned int length) {
  Serial.printf("Message on %s: %.*s\n", topic, length, payload);
  // Handle commands here
}

void publishSensor(const char* type, float value) {
  char topic[64], msg[32];
  snprintf(topic, sizeof(topic), "sensors/%s/%s", DEVICE_ID, type);
  snprintf(msg, sizeof(msg), "%.2f", value);
  mqtt.publish(topic, msg);
}

void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) delay(500);
  connectMQTT();
}

void loop() {
  if (!mqtt.connected()) connectMQTT();
  mqtt.loop();

  // Example: publish temperature every 30s
  static unsigned long lastPublish = 0;
  if (millis() - lastPublish > 30000) {
    publishSensor("temperature", 23.5);
    publishSensor("humidity", 65.0);
    lastPublish = millis();
  }
}
```

### Dynamic CA Certificate Fetching

The CA certificate is automatically uploaded to S3 when Mosquitto starts. Devices can fetch it dynamically:

**URL**: `https://itercraft-mqtt-certs.s3.eu-west-1.amazonaws.com/mqtt/ca.crt`

```cpp
#include <HTTPClient.h>
#include <Preferences.h>

#define CA_CERT_URL "https://itercraft-mqtt-certs.s3.eu-west-1.amazonaws.com/mqtt/ca.crt"

Preferences prefs;
String caCert;

bool fetchCACert() {
  HTTPClient http;
  http.begin(CA_CERT_URL);
  int code = http.GET();

  if (code == 200) {
    caCert = http.getString();
    // Cache in NVS for offline use
    prefs.begin("mqtt", false);
    prefs.putString("ca_cert", caCert);
    prefs.end();
    Serial.println("CA cert fetched and cached");
    return true;
  }
  Serial.printf("Failed to fetch CA cert: %d\n", code);
  return false;
}

void loadCACert() {
  prefs.begin("mqtt", true);
  caCert = prefs.getString("ca_cert", "");
  prefs.end();

  if (caCert.isEmpty()) {
    fetchCACert();
  }
}

void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) delay(500);

  loadCACert();
  espClient.setCACert(caCert.c_str());
  connectMQTT();
}
```

**Certificate refresh**: Call `fetchCACert()` periodically (e.g., daily) or when MQTT connection fails with TLS errors.

### Device Decommissioning

```bash
cd iot/mosquitto

# Remove device from password file
# (mosquitto_passwd doesn't have delete, so recreate without the device)
grep -v "^device-esp32-ABC123:" passwd > passwd.tmp && mv passwd.tmp passwd

# Restart Mosquitto to apply changes
docker restart itercraft-mosquitto
```

### Run Locally (Development)

```bash
cd iot/mosquitto
docker-compose up -d

# Test with mosquitto_pub
mosquitto_pub -h localhost -p 8883 \
  --cafile certs/ca.crt \
  -u device-esp32-test -P testpassword \
  -t "sensors/esp32-test/temperature" \
  -m "25.3"
```

### Build & Push to ECR

```bash
cd iot/mosquitto
docker build -t itercraft-mosquitto .

# Tag and push
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin <account_id>.dkr.ecr.eu-west-1.amazonaws.com
docker tag itercraft-mosquitto:latest <account_id>.dkr.ecr.eu-west-1.amazonaws.com/itercraft_mosquitto:latest
docker push <account_id>.dkr.ecr.eu-west-1.amazonaws.com/itercraft_mosquitto:latest
```

## License

Proprietary - All rights reserved.

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
  required_version = ">= 1.5.0"

  backend "s3" {
    # Configuration via -backend-config dans terraform init
    # bucket         = "itercraft-terraform-state"
    # key            = "aws_ec2/terraform.tfstate"
    # region         = "eu-west-1"
    # dynamodb_table = "itercraft-terraform-locks"
    # encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
}

provider "cloudflare" {
  api_token = var.cloudflare_api_token
}

# AMI Ubuntu 22.04 LTS
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

# Cloudflare IPs
data "http" "cloudflare_ipv4" {
  url = "https://www.cloudflare.com/ips-v4"
}

data "http" "cloudflare_ipv6" {
  url = "https://www.cloudflare.com/ips-v6"
}

locals {
  cloudflare_ips = concat(
    split("\n", trimspace(data.http.cloudflare_ipv4.response_body)),
    split("\n", trimspace(data.http.cloudflare_ipv6.response_body))
  )

  cloudflare_ipv4 = [for ip in local.cloudflare_ips : ip if can(regex("^\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+$", ip))]
  cloudflare_ipv6 = [for ip in local.cloudflare_ips : ip if can(regex("^[0-9a-fA-F:]+/\\d+$", ip))]
}

resource "aws_security_group" "app_sg" {
  name        = "app-sg"
  description = "SG EC2 app behind Cloudflare"
  vpc_id      = var.vpc_id

  # IPv4 HTTP Cloudflare
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = local.cloudflare_ipv4
  }

  # IPv6 HTTP Cloudflare
  ingress {
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    ipv6_cidr_blocks = local.cloudflare_ipv6
  }

  # MQTT over TLS (direct, no Cloudflare proxy)
  # Security: TLS 1.3 + password authentication + ACL
  ingress {
    from_port   = 8883
    to_port     = 8883
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "MQTT over TLS for IoT devices"
  }

  # Sortie
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "app-sg"
  }
}

resource "aws_iam_role" "ec2_ecr_role" {
  name = "ec2-ecr-readonly"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        },
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_ecr_attach" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "ec2_ssm_attach" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Allow Mosquitto to upload CA certificate to S3
resource "aws_iam_role_policy" "ec2_mqtt_s3" {
  name = "mqtt-cert-upload"
  role = aws_iam_role.ec2_ecr_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:PutObjectAcl"]
        Resource = "arn:aws:s3:::itercraft-mqtt-certs/mqtt/ca.crt"
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2_ecr_profile" {
  name = "ec2-ecr-profile"
  role = aws_iam_role.ec2_ecr_role.name
}

resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = "app-eip"
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  associate_public_ip_address = true

  iam_instance_profile   = aws_iam_instance_profile.ec2_ecr_profile.name

  root_block_device {
    volume_size = 40
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              apt update && apt install -y docker.io docker-compose awscli
              systemctl enable docker

              # Configure Docker daemon to include compose labels in log attrs
              # Required for Promtail to extract container_name from Docker JSON logs
              mkdir -p /etc/docker
              cat > /etc/docker/daemon.json << DAEMON
              {
                "log-driver": "json-file",
                "log-opts": {
                  "labels": "com.docker.compose.service,com.docker.compose.project"
                }
              }
              DAEMON
              systemctl restart docker

              mkdir -p /opt/app && cd /opt/app

              # Login ECR automatique
              aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com

              # docker-compose.yml
              cat > docker-compose.yml << EOL
              version: "3.9"
              networks:
                public:
                  driver: bridge
                internal:
                  driver: bridge
              services:
                traefik:
                  image: traefik:v3.0
                  command:
                    - "--providers.docker=true"
                    - "--providers.docker.exposedbydefault=false"
                    - "--entrypoints.web.address=:80"
                    - "--entrypoints.web.forwardedHeaders.trustedIPs=173.245.48.0/20,103.21.244.0/22,103.22.200.0/22,103.31.4.0/22,141.101.64.0/18,108.162.192.0/18,190.93.240.0/20,188.114.96.0/20,197.234.240.0/22,198.41.128.0/17,162.158.0.0/15,104.16.0.0/13,104.24.0.0/14,172.64.0.0/13,131.0.72.0/22"
                  ports:
                    - "80:80"
                  volumes:
                    - /var/run/docker.sock:/var/run/docker.sock:ro
                  networks:
                    - public
                  restart: always

                front:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_front:latest
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.front.rule=Host(\`www.${var.domain_name}\`)"
                    - "traefik.http.routers.front.entrypoints=web"
                    - "traefik.http.routers.front.middlewares=https-proto"
                    - "traefik.http.services.front.loadbalancer.server.port=3000"
                  networks:
                    - public
                  restart: always

                back:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_api:latest
                  depends_on:
                    bdd:
                      condition: service_healthy
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.back.rule=Host(\`api.${var.domain_name}\`)"
                    - "traefik.http.routers.back.entrypoints=web"
                    - "traefik.http.routers.back.middlewares=https-proto"
                    - "traefik.http.services.back.loadbalancer.server.port=8080"
                    - "traefik.docker.network=app_public"
                  environment:
                    - DB_HOST=bdd
                    - DB_PASSWORD=${var.db_password}
                    - KEYCLOAK_INTERNAL_URL=http://authent:8180
                    - KEYCLOAK_ISSUER_URI=https://authent.${var.domain_name}/realms/itercraft
                    - CORS_ORIGINS=https://www.${var.domain_name}
                    - METEOFRANCE_API_TOKEN=${var.meteo_api_key}
                    - ANTHROPIC_API_KEY=${var.anthropic_api_key}
                    - MQTT_HOST=mosquitto
                    - MQTT_PORT=8883
                    - MQTT_BACKEND_USER=itercraft-backend
                    - MQTT_BACKEND_PASSWORD=${var.mqtt_backend_password}
                    - MQTT_TRUST_ALL_CERTS=true
                    - OTLP_ENDPOINT=http://tempo:4318/v1/traces
                  networks:
                    - public
                    - internal
                  restart: always

                authent:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_authent:latest
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.authent.rule=Host(\`authent.${var.domain_name}\`)"
                    - "traefik.http.routers.authent.entrypoints=web"
                    - "traefik.http.routers.authent.middlewares=https-proto"
                    - "traefik.http.middlewares.https-proto.headers.customrequestheaders.X-Forwarded-Proto=https"
                    - "traefik.http.services.authent.loadbalancer.server.port=8180"
                    - "traefik.docker.network=app_public"
                  environment:
                    - KC_BOOTSTRAP_ADMIN_PASSWORD=${var.keycloak_admin_password}
                    - KEYCLOAK_HEALTHCHECK_PASSWORD=${var.keycloak_healthcheck_password}
                    - KC_HOSTNAME=https://authent.${var.domain_name}
                    - KC_PROXY_HEADERS=xforwarded
                    - KC_HTTP_ENABLED=true
                    - KC_HOSTNAME_STRICT=false
                    - KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true
                  networks:
                    - public
                    - internal
                  restart: always

                bdd:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_bdd:latest
                  environment:
                    - POSTGRES_PASSWORD=${var.db_password}
                  healthcheck:
                    test: ["CMD-SHELL", "pg_isready -U itercraft"]
                    interval: 5s
                    timeout: 3s
                    retries: 5
                  volumes:
                    - pgdata:/var/lib/postgresql/data
                  networks:
                    - internal
                  restart: always

                prometheus:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_prometheus:latest
                  networks:
                    - internal
                  restart: always

                grafana:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_grafana:latest
                  environment:
                    - GF_SECURITY_ADMIN_PASSWORD=${var.grafana_admin_password}
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.grafana.rule=Host(\`grafana.${var.domain_name}\`)"
                    - "traefik.http.routers.grafana.entrypoints=web"
                    - "traefik.http.routers.grafana.middlewares=https-proto"
                    - "traefik.http.services.grafana.loadbalancer.server.port=3001"
                    - "traefik.docker.network=app_public"
                  networks:
                    - public
                    - internal
                  restart: always

                mosquitto:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_mosquitto:latest
                  ports:
                    - "8883:8883"
                  environment:
                    - MQTT_USER=${var.mqtt_user}
                    - MQTT_PASSWORD=${var.mqtt_password}
                    - MQTT_BACKEND_USER=itercraft-backend
                    - MQTT_BACKEND_PASSWORD=${var.mqtt_backend_password}
                    - S3_CA_BUCKET=itercraft-mqtt-certs
                    - AWS_DEFAULT_REGION=${var.aws_region}
                  volumes:
                    - mosquitto-data:/mosquitto/data
                    - mosquitto-certs:/mosquitto/config/certs
                  networks:
                    - internal
                  restart: always
                  healthcheck:
                    test: ["CMD-SHELL", "echo | openssl s_client -connect localhost:8883 -CAfile /mosquitto/config/certs/ca.crt 2>&1 | grep -q CONNECTED"]
                    interval: 30s
                    timeout: 10s
                    retries: 3

                loki:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_loki:latest
                  volumes:
                    - loki-data:/loki
                  networks:
                    - internal
                  restart: always
                  healthcheck:
                    test: ["CMD-SHELL", "wget -q --spider http://localhost:3100/ready || exit 1"]
                    interval: 30s
                    timeout: 10s
                    retries: 3

                promtail:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_promtail:latest
                  volumes:
                    - /var/lib/docker/containers:/var/lib/docker/containers:ro
                    - /var/log:/var/log:ro
                  depends_on:
                    loki:
                      condition: service_healthy
                  networks:
                    - internal
                  restart: always

                tempo:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_tempo:latest
                  volumes:
                    - tempo-data:/var/tempo
                  networks:
                    - internal
                  restart: always
                  healthcheck:
                    test: ["CMD-SHELL", "wget -q --spider http://localhost:3200/ready || exit 1"]
                    interval: 30s
                    timeout: 10s
                    retries: 3

                falcosidekick:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_falcosidekick:latest
                  environment:
                    - SLACK_WEBHOOK_URL=${var.slack_webhook_url}
                  depends_on:
                    loki:
                      condition: service_healthy
                  networks:
                    - internal
                  restart: always

                falco:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_falco:latest
                  privileged: true
                  volumes:
                    - /var/run/docker.sock:/var/run/docker.sock:ro
                    - /dev:/host/dev:ro
                    - /proc:/host/proc:ro
                    - /etc:/host/etc:ro
                    - falco-logs:/var/log/falco
                  depends_on:
                    - falcosidekick
                  networks:
                    - internal
                  restart: always
              volumes:
                pgdata:
                mosquitto-data:
                mosquitto-certs:
                loki-data:
                tempo-data:
                falco-logs:
              EOL

              docker-compose up -d
              EOF

  tags = {
    Name = "app-ec2"
  }
}

output "elastic_ip" {
  description = "Elastic IP of the EC2 instance"
  value       = aws_eip.app.public_ip
}

# --- Cloudflare DNS ---

data "cloudflare_zone" "main" {
  name = var.domain_name
}

resource "cloudflare_record" "apex" {
  zone_id = data.cloudflare_zone.main.id
  name    = "@"
  content = aws_eip.app.public_ip
  type    = "A"
  proxied = true
}

resource "cloudflare_record" "www" {
  zone_id = data.cloudflare_zone.main.id
  name    = "www"
  content = aws_eip.app.public_ip
  type    = "A"
  proxied = true
}

resource "cloudflare_record" "api" {
  zone_id = data.cloudflare_zone.main.id
  name    = "api"
  content = aws_eip.app.public_ip
  type    = "A"
  proxied = true
}

resource "cloudflare_record" "authent" {
  zone_id = data.cloudflare_zone.main.id
  name    = "authent"
  content = aws_eip.app.public_ip
  type    = "A"
  proxied = true
}

resource "cloudflare_record" "grafana" {
  zone_id = data.cloudflare_zone.main.id
  name    = "grafana"
  content = aws_eip.app.public_ip
  type    = "A"
  proxied = true
}

# MQTT broker - DNS only (no proxy, MQTT is TCP not HTTP)
resource "cloudflare_record" "mqtt" {
  zone_id = data.cloudflare_zone.main.id
  name    = "mqtt"
  content = aws_eip.app.public_ip
  type    = "A"
  proxied = false
}

resource "cloudflare_zone_settings_override" "ssl" {
  zone_id = data.cloudflare_zone.main.id

  settings {
    ssl                      = "flexible"
    always_use_https         = "on"
    automatic_https_rewrites = "on"
  }
}

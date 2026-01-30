terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.5.0"
}

provider "aws" {
  region = var.aws_region
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
  description = "SG EC2 app derrière Cloudflare"
  vpc_id      = var.vpc_id

  # IPv4 HTTP/HTTPS Cloudflare
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = local.cloudflare_ipv4
  }
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = local.cloudflare_ipv4
  }

  # IPv6 HTTP/HTTPS Cloudflare
  ingress {
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    ipv6_cidr_blocks = local.cloudflare_ipv6
  }
  ingress {
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    ipv6_cidr_blocks = local.cloudflare_ipv6
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

resource "aws_iam_instance_profile" "ec2_ecr_profile" {
  name = "ec2-ecr-profile"
  role = aws_iam_role.ec2_ecr_role.name
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
                    - "--entrypoints.websecure.address=:443"
                    - "--certificatesresolvers.le.acme.httpchallenge=true"
                    - "--certificatesresolvers.le.acme.httpchallenge.entrypoint=web"
                    - "--certificatesresolvers.le.acme.email=admin@${var.domain_name}"
                    - "--certificatesresolvers.le.acme.storage=/letsencrypt/acme.json"
                  ports:
                    - "80:80"
                    - "443:443"
                  volumes:
                    - /var/run/docker.sock:/var/run/docker.sock:ro
                    - ./letsencrypt:/letsencrypt
                  networks:
                    - public
                  restart: always

                front:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_front:latest
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.front.rule=Host(\`www.${var.domain_name}\`)"
                    - "traefik.http.routers.front.entrypoints=websecure"
                    - "traefik.http.routers.front.tls.certresolver=le"
                  networks:
                    - public
                  restart: always

                back:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_api:latest
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.back.rule=Host(\`api.${var.domain_name}\`)"
                    - "traefik.http.routers.back.entrypoints=websecure"
                    - "traefik.http.routers.back.tls.certresolver=le"
                  environment:
                    - DB_HOST=bdd
                    - DB_PASSWORD=${var.db_password}
                    - KEYCLOAK_URL=http://authent:8180
                    - KEYCLOAK_CLIENT_SECRET=${var.keycloak_client_secret}
                    - CORS_ORIGINS=https://www.${var.domain_name}
                  networks:
                    - public
                    - internal
                  restart: always

                authent:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_authent:latest
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.authent.rule=Host(\`authent.${var.domain_name}\`)"
                    - "traefik.http.routers.authent.entrypoints=websecure"
                    - "traefik.http.routers.authent.tls.certresolver=le"
                  environment:
                    - KC_HOSTNAME=authent.${var.domain_name}
                    - KC_HOSTNAME_PORT=-1
                  networks:
                    - public
                    - internal
                  restart: always

                bdd:
                  image: ${var.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/itercraft_bdd:latest
                  environment:
                    - POSTGRES_PASSWORD=${var.db_password}
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
                  labels:
                    - "traefik.enable=true"
                    - "traefik.http.routers.grafana.rule=Host(\`grafana.${var.domain_name}\`)"
                    - "traefik.http.routers.grafana.entrypoints=websecure"
                    - "traefik.http.routers.grafana.tls.certresolver=le"
                    - "traefik.http.services.grafana.loadbalancer.server.port=3001"
                  networks:
                    - public
                    - internal
                  restart: always
              volumes:
                pgdata:
              EOL

              docker-compose up -d
              EOF

  tags = {
    Name = "app-ec2"
  }
}

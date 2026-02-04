variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "eu-west-1"
}

variable "instance_type" {
  description = "Type EC2"
  type        = string
  default     = "t3.large"
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_id" {
  description = "Subnet ID"
  type        = string
}

variable "account_id" {
  description = "Account ID"
  type        = string
}

variable "domain_name" {
  description = "Domain name (e.g. itercraft.com)"
  type        = string
}

variable "db_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "keycloak_client_secret" {
  description = "Keycloak iterapi client secret"
  type        = string
  sensitive   = true
}

variable "cloudflare_api_token" {
  description = "Cloudflare API token with DNS edit and Zone Settings edit permissions"
  type        = string
  sensitive   = true
}

variable "meteo_api_key" {
  description = "MeteoFrance API Key"
  type        = string
  sensitive   = true
}

variable "anthropic_api_key" {
  description = "Anthropic API Key (Claude)"
  type        = string
  sensitive   = true
}

variable "keycloak_healthcheck_password" {
  description = "Keycloak healthcheck user password"
  type        = string
  sensitive   = true
}

variable "keycloak_admin_password" {
  description = "Keycloak admin password"
  type        = string
  sensitive   = true
}

variable "grafana_admin_password" {
  description = "Grafana admin password"
  type        = string
  sensitive   = true
}
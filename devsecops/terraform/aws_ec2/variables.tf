variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "eu-west-1"
}

variable "instance_type" {
  description = "Type EC2"
  type        = string
  default     = "t3a.medium"
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
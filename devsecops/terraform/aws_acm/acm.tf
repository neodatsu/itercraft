terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_acm_certificate" "main" {
  domain_name               = var.domain_name
  subject_alternative_names = ["*.${var.domain_name}"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = var.domain_name
  }
}

output "certificate_arn" {
  description = "ARN of the certificate"
  value       = aws_acm_certificate.main.arn
}

output "domain_validation_options" {
  description = "DNS records to create for validation"
  value       = aws_acm_certificate.main.domain_validation_options
}

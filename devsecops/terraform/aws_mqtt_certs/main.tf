terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  default = "eu-west-1"
}

# S3 bucket for MQTT CA certificate (public read, minimal cost)
resource "aws_s3_bucket" "mqtt_certs" {
  bucket = "itercraft-mqtt-certs"
}

# Explicitly disable versioning (cost savings)
resource "aws_s3_bucket_versioning" "mqtt_certs" {
  bucket = aws_s3_bucket.mqtt_certs.id
  versioning_configuration {
    status = "Disabled"
  }
}

resource "aws_s3_bucket_public_access_block" "mqtt_certs" {
  bucket = aws_s3_bucket.mqtt_certs.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "mqtt_certs_public" {
  bucket = aws_s3_bucket.mqtt_certs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadCA"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.mqtt_certs.arn}/mqtt/ca.crt"
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.mqtt_certs]
}

# IAM policy for Mosquitto container to upload CA cert
resource "aws_iam_policy" "mqtt_cert_upload" {
  name        = "itercraft-mqtt-cert-upload"
  description = "Allow Mosquitto to upload CA certificate to S3"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.mqtt_certs.arn}/mqtt/ca.crt"
      },
      {
        Effect   = "Allow"
        Action   = "s3:PutObjectAcl"
        Resource = "${aws_s3_bucket.mqtt_certs.arn}/mqtt/ca.crt"
      }
    ]
  })
}

output "ca_cert_url" {
  description = "Public URL to fetch the MQTT CA certificate"
  value       = "https://${aws_s3_bucket.mqtt_certs.bucket_regional_domain_name}/mqtt/ca.crt"
}

output "bucket_name" {
  description = "S3 bucket name for MQTT certificates"
  value       = aws_s3_bucket.mqtt_certs.bucket
}

variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "eu-west-1"
}

variable "account_id" {
  description = "AWS Account ID"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository (e.g. neodatsu/itercraft)"
  type        = string
}

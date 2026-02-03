variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "eu-west-1"
}

variable "github_token" {
  description = "GitHub Personal Access Token with workflow dispatch permission"
  type        = string
  sensitive   = true
}

variable "github_repo" {
  description = "GitHub repository (e.g. neodatsu/itercraft)"
  type        = string
  default     = "neodatsu/itercraft"
}

variable "slack_signing_secret" {
  description = "Slack app signing secret for request verification"
  type        = string
  sensitive   = true
}

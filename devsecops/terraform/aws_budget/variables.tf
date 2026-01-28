variable "alert_email" {
  description = "Email address for budget alerts"
  type        = string
  sensitive   = true
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

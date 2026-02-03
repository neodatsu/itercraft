terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend S3 configurable via -backend-config (après création de aws_backend)
  # Pour un premier déploiement local, commenter le bloc backend ci-dessous
  # backend "s3" {}
}

provider "aws" {
  region = var.aws_region
}

# Lambda function for Slack → GitHub Actions bridge
resource "aws_lambda_function" "slack_github_bridge" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = "itercraft-slack-github-bridge"
  role             = aws_iam_role.lambda_role.arn
  handler          = "index.handler"
  runtime          = "nodejs20.x"
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  timeout          = 10

  environment {
    variables = {
      GITHUB_TOKEN      = var.github_token
      GITHUB_REPO       = var.github_repo
      SLACK_SIGNING_SECRET = var.slack_signing_secret
    }
  }
}

# Lambda source code
data "archive_file" "lambda_zip" {
  type        = "zip"
  source_dir  = "${path.module}/src"
  output_path = "${path.module}/lambda.zip"
}

# IAM role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "itercraft-slack-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Basic Lambda execution policy
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# API Gateway for Slack webhook
resource "aws_apigatewayv2_api" "slack_api" {
  name          = "itercraft-slack-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.slack_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_apigatewayv2_integration" "lambda" {
  api_id                 = aws_apigatewayv2_api.slack_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.slack_github_bridge.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "slack" {
  api_id    = aws_apigatewayv2_api.slack_api.id
  route_key = "POST /slack/infra"
  target    = "integrations/${aws_apigatewayv2_integration.lambda.id}"
}

# Permission for API Gateway to invoke Lambda
resource "aws_lambda_permission" "apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.slack_github_bridge.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.slack_api.execution_arn}/*/*"
}

output "slack_webhook_url" {
  description = "URL to configure in Slack slash command"
  value       = "${aws_apigatewayv2_api.slack_api.api_endpoint}/slack/infra"
}

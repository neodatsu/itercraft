#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/env.sh"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: env.sh not found at $ENV_FILE"
    exit 1
fi

source "$ENV_FILE"

# Function to get MFA session token
get_mfa_session() {
    if [[ -z "$AWS_MFA_SERIAL" || "$AWS_MFA_SERIAL" == *"ACCOUNT_ID"* ]]; then
        echo "Warning: MFA not configured, skipping MFA authentication"
        return 0
    fi

    read -p "Enter MFA token: " MFA_TOKEN

    echo "Getting session token with MFA..."
    CREDS=$(aws sts get-session-token \
        --serial-number "$AWS_MFA_SERIAL" \
        --token-code "$MFA_TOKEN" \
        --output json)

    export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r '.Credentials.AccessKeyId')
    export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r '.Credentials.SecretAccessKey')
    export AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r '.Credentials.SessionToken')

    echo "MFA session established (expires: $(echo "$CREDS" | jq -r '.Credentials.Expiration'))"
}

show_usage() {
    echo "Usage: $0 <module> <command> [options]"
    echo ""
    echo "  module:  aws_budget, ..."
    echo "  command: init, plan, apply, destroy"
    echo ""
    echo "Examples:"
    echo "  $0 aws_budget init"
    echo "  $0 aws_budget plan"
    echo "  $0 aws_budget apply"
    echo "  $0 aws_budget destroy"
}

if [[ -z "$1" || -z "$2" ]]; then
    show_usage
    exit 1
fi

MODULE="$1"
COMMAND="$2"
shift 2

MODULE_DIR="$SCRIPT_DIR/$MODULE"

if [[ ! -d "$MODULE_DIR" ]]; then
    echo "Error: Module '$MODULE' not found at $MODULE_DIR"
    exit 1
fi

# Request MFA for apply and destroy operations
case "$COMMAND" in
    apply|destroy)
        get_mfa_session
        ;;
esac

cd "$MODULE_DIR"

case "$COMMAND" in
    init)
        terraform init "$@"
        ;;
    plan)
        terraform plan "$@"
        ;;
    apply)
        terraform apply "$@"
        ;;
    destroy)
        terraform destroy "$@"
        ;;
    *)
        echo "Error: Unknown command '$COMMAND'"
        show_usage
        exit 1
        ;;
esac

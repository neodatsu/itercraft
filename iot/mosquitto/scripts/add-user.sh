#!/bin/bash
# Add a user to Mosquitto password file
# Usage: ./add-user.sh <username> [password]

set -euo pipefail

PASSWD_FILE="$(dirname "$0")/../passwd"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <username> [password]"
    echo "If password is not provided, you will be prompted"
    exit 1
fi

USERNAME="$1"

# Create passwd file if it doesn't exist
touch "$PASSWD_FILE"

if [ $# -ge 2 ]; then
    # Password provided as argument (for automation)
    PASSWORD="$2"
    # Use mosquitto_passwd in batch mode
    mosquitto_passwd -b "$PASSWD_FILE" "$USERNAME" "$PASSWORD"
else
    # Interactive mode
    mosquitto_passwd "$PASSWD_FILE" "$USERNAME"
fi

echo "User '$USERNAME' added/updated in $PASSWD_FILE"

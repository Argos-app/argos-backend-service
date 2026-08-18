#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

ENV_NAME="${1:-prod}"

case "$ENV_NAME" in
  prod)
    CONFIG_SOURCE="discloud.config"
    ;;
  homolog)
    CONFIG_SOURCE="discloud.config.homolog"
    ;;
  *)
    echo "Unknown environment: '$ENV_NAME' (expected: prod|homolog)"
    exit 1
    ;;
esac

BACKUP_CONFIG="discloud.config.bak"

cleanup() {
  if [ "$CONFIG_SOURCE" != "discloud.config" ] && [ -f "$BACKUP_CONFIG" ]; then
    echo "==> Restoring discloud.config..."
    mv "$BACKUP_CONFIG" discloud.config
  fi
  rm -f app.jar
}
trap cleanup EXIT

echo "==> Building application ($ENV_NAME)..."
./mvnw -B clean package -DskipTests

echo "==> Staging app.jar for Discloud upload..."
cp target/app.jar app.jar

if [ "$CONFIG_SOURCE" != "discloud.config" ]; then
  echo "==> Staging $CONFIG_SOURCE as discloud.config..."
  cp discloud.config "$BACKUP_CONFIG"
  cp "$CONFIG_SOURCE" discloud.config
fi

APP_ID="$(grep -E '^ID=' discloud.config | head -n 1 | cut -d= -f2)"

if discloud app info "$APP_ID" >/dev/null 2>&1; then
  echo "==> App '$APP_ID' already exists, committing new build..."
  discloud app commit "$APP_ID"
else
  echo "==> App '$APP_ID' not found, uploading as a new app..."
  discloud app up
fi

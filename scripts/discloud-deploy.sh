#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Building application..."
./mvnw -B clean package -DskipTests

echo "==> Staging app.jar for Discloud upload..."
cp target/app.jar app.jar

echo "==> Deploying to Discloud..."
discloud up

echo "==> Cleaning staged jar..."
rm -f app.jar

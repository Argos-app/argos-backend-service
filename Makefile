# Author: @luismede
# Version: 1.0

JAVA_VERSION = $(shell java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

.PHONY: up down restart logs ps clean run test format lint deploy deploy-homolog

check-java:
	@if [ -z "$(JAVA_VERSION)" ]; then \
		echo "Java is not installed."; \
		echo "Download in: https://adoptium.net"; \
		exit 1; \
	elif [ "$(JAVA_VERSION)" -lt 21 ]; then \
		echo "Java 21 or higher is required. Current version: $(JAVA_VERSION)"; \
		echo "Download in: https://adoptium.net)"; \
		exit 1; \
	fi

## Up all services in Background
up:
	docker compose up -d

## Stop all services
down:
	docker compose down

## Restart all services
restart:
	docker compose restart

## Show logs in real time (ctrl-c to exit)
logs:
	docker compose logs -f

## List the status of the containers
ps:
	docker compose ps -a

## Stop and remove containers, volumes and networks
clean:
	docker compose down -v --remove-orphans

## Run application
run: check-java
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.useTestClasspath=false

## Run tests
test: check-java
	./mvnw -B test -Dspring.profiles.active=test

## Format code
format: check-java
	./mvnw spotless:apply

## Scan code to find bad praticies
lint: check-java
	./mvnw pmd:check

## Deploy to Discloud (production)
deploy:
	./scripts/discloud-deploy.sh prod

## Deploy to Discloud (homologation/staging)
deploy-homolog:
	./scripts/discloud-deploy.sh homolog

.PHONY: up down restart logs ps clean run test format lint deploy deploy-homolog check-java

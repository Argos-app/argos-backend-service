# Author: @luismede
# Version: 1.0

.PHONY up down restart logs ps clean

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
	docker compose -v --remove-orphans
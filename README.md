# RSO: Računi microservice

## Prerequisites

Create pg_
```bash
docker run -d --name pg-racuni -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=racuni -p 5432:54
32 --network rsonet postgres:13
```
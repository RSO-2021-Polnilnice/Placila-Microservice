# RSO: Računi microservice

## Prerequisites

Create database container
```bash
docker run -d --name pg-racuni -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=racuni -p 5434:5432 postgres:13
```
Create docker container 
```shell
docker run -p 8083:8080 --network rsonet -e KUMULUZEE_DATASOURCES0_CONNECTIONURL=jdbc:postgresql://192.168.99.100:5434/racuni -e KUMULUZEE_CONFIG_CONSUL_AGENT=http://192.168.99.100:8500 --name placila-instance placila
```
Start database and admin-ms containers
```shell
docker start pg-racuni
docker start placila-instance
```
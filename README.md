# parts-vibe

Spring Boot + Thymeleaf + PostgreSQL skeleton with JPA, basic role-based auth, and Solr-backed catalog indexing/search.

## Project layout
Multi-module Maven project:
- `webapp` (Spring Boot app + web layer)
- `application` (application/services)
- `data-access` (JPA entities + repositories)
- `search` (Solr integration)
- `shared` (shared utilities, currently empty)

## Requirements
- JDK 25
- Maven 3.9+
- PostgreSQL 15+
- Docker (for Testcontainers integration tests)

## Configure Postgres
Create a database and set credentials, or use the defaults from `application.yml`:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=webapp
DB_USER=webapp
DB_PASSWORD=webapp
```

## Start Postgres with Docker Compose
Use the bundled `docker-compose/docker-compose.yml` to spin up Postgres locally (includes pgAdmin, Solr, Prometheus, and Grafana):

```
docker compose -f docker-compose/docker-compose.yml up -d
```

Check health:

```
docker compose -f docker-compose/docker-compose.yml ps
```

Stop:

```
docker compose -f docker-compose/docker-compose.yml down
```

## Access pgAdmin
- URL: http://localhost:5050
- Email: `admin@localtest.me`
- Password: `admin123`
- Add server connection:
  - Host: `db`
  - Port: `5432`
  - Maintenance DB: `webapp`
  - Username: `webapp`
  - Password: `webapp`

## Access Solr Admin UI
- URL: http://localhost:8983/solr
- Core: `catalog` (auto-created by docker-compose)
- Check docs: open the core, then run a query like `q=*:*` in the Query tab.

## Metrics (Prometheus + Grafana)
- Prometheus: http://localhost:9090 (scrapes `http://host.docker.internal:8080/actuator/prometheus`)
- Grafana: http://localhost:3000 (admin/admin)
- Dashboard: “PartsVibe JVM” is provisioned on first start.

## Logs (Loki + Grafana)
- Loki: http://localhost:3100 (Grafana data source is auto-provisioned)
- Logs in Grafana: Explore → select Loki → `{container="webapp"}` (adjust container name as needed).
- Loki readiness check: http://localhost:3100/ready

## Default users (seeded at startup)
- Admin: `admin` / `admin123` (ROLE_ADMIN + ROLE_USER)
- User: `user` / `user123` (ROLE_USER)

Override with:

```
APP_ADMIN_USERNAME=...
APP_ADMIN_PASSWORD=...
APP_USER_USERNAME=...
APP_USER_PASSWORD=...
```

## Run the app
```
./mvnw -pl webapp spring-boot:run
```

To run with the dev profile (auto-create/update schema from JPA entities):
```
SPRING_PROFILES_ACTIVE=dev ./mvnw -pl webapp spring-boot:run
```

## Generate DB schema from code
The `dev` profile sets `spring.jpa.hibernate.ddl-auto=update`, which generates/updates tables from your entities.

Options:
- `update` (safe-ish for dev): `SPRING_JPA_HIBERNATE_DDL_AUTO=update ./mvnw -pl webapp spring-boot:run`
- `create-drop` (clean rebuild each run): `SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop ./mvnw -pl webapp spring-boot:run`

## Run unit tests
```
./mvnw test
```

## Run integration tests (Testcontainers)
```
./mvnw -Pintegration-test verify
```

## Routes
- `/` public home
- `/login` login page
- `/contact` ROLE_USER
- `/catalog/index` ROLE_USER (indexes text into Solr)
- `/catalog/search` ROLE_USER (search Solr)
- `/admin` ROLE_ADMIN

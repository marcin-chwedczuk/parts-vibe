# parts-vibe
![CI](https://github.com/marcin-chwedczuk/parts-vibe/actions/workflows/ci.yml/badge.svg)

Spring Boot + Thymeleaf + PostgreSQL skeleton with JPA, basic role-based auth, and Solr-backed catalog indexing/search.

## Project layout
Multi-module Maven project:
- `app` (Spring Boot app + runtime assembly)
- `catalog` (catalog web flow)
- `users` (RBAC + users)
- `site` (home + contact)
- `search-api` (search contracts)
- `search-impl` (Solr-backed search implementation)
- `shared` (shared utilities, currently empty)

## Requirements
- JDK 25
- Maven 3.9+
- PostgreSQL 15+
- Docker (for Testcontainers integration tests)

## Maven repository mirror (optional)
If you want resilience against Maven Central outages, configure a local mirror in
`~/.m2/settings.xml` (or in CI). This keeps project POMs clean and reproducible
while allowing a controlled fallback (e.g., a Nexus or Artifactory proxy).

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
./mvnw -pl app spring-boot:run
```

To run with the dev profile (auto-create/update schema from JPA entities):
```
SPRING_PROFILES_ACTIVE=dev ./mvnw -pl app spring-boot:run
```

## Generate DB schema from code
The `dev` profile sets `spring.jpa.hibernate.ddl-auto=update`, which generates/updates tables from your entities.

Options:
- `update` (safe-ish for dev): `SPRING_JPA_HIBERNATE_DDL_AUTO=update ./mvnw -pl app spring-boot:run`
- `create-drop` (clean rebuild each run): `SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop ./mvnw -pl app spring-boot:run`

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

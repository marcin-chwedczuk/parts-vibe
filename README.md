# webapp

Spring Boot + Thymeleaf + PostgreSQL skeleton with JPA, basic role-based auth, and a validated contact form.

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
Use the bundled `docker/docker-compose.yml` to spin up Postgres locally:

```
docker compose -f docker/docker-compose.yml up -d
```

Check health:

```
docker compose -f docker/docker-compose.yml ps
```

Stop:

```
docker compose -f docker/docker-compose.yml down
```

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
mvn spring-boot:run
```

To run with the dev profile (auto-create/update schema from JPA entities):
```
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

## Generate DB schema from code
The `dev` profile sets `spring.jpa.hibernate.ddl-auto=update`, which generates/updates tables from your entities.

Options:
- `update` (safe-ish for dev): `SPRING_JPA_HIBERNATE_DDL_AUTO=update mvn spring-boot:run`
- `create-drop` (clean rebuild each run): `SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop mvn spring-boot:run`

## Run unit tests
```
mvn test
```

## Run integration tests (Testcontainers)
```
mvn -Pintegration-test verify
```

## Routes
- `/` public home
- `/login` login page
- `/contact` ROLE_USER
- `/admin` ROLE_ADMIN

# Last Session Notes

## Current State
- Project: Spring Boot 4.0.2, Java 25, Maven wrapper in repo.
- Docker compose moved to `docker/docker-compose.yml`.
- Services: Postgres (`db`) + pgAdmin (`pgadmin`).
- Postgres data dir: `docker/db/` (gitignored).
- pgAdmin data dir: `docker/pgadmin/` (gitignored).

## Known Behavior
- Default profile uses `spring.jpa.hibernate.ddl-auto=validate` which fails if tables are missing.
- `dev` profile sets `ddl-auto=update`, which fixes missing-table errors for local dev.

## Usage Reminders
- Always use `./mvnw` for Maven commands.
- Compose commands: `docker compose -f docker/docker-compose.yml up -d`.
- pgAdmin: http://localhost:5050 (admin@local.test / admin123); connect to host `db`.

## Recent Files Updated
- `docker/docker-compose.yml`
- `README.md`
- `.gitignore`
- `AGENTS.md`

# Agent Instructions

## Project Basics
- Project: `webapp`
- Language: Java 25
- Build tool: Maven
- Framework: Spring Boot + Thymeleaf
- Database: PostgreSQL

## Command Rules
- Always use the Maven Wrapper for Maven-related commands: `./mvnw` (not `mvn`).
- Prefer `./mvnw -q` only when output noise is a problem; otherwise keep normal verbosity.
- Do not run Maven commands unless asked or needed to verify a change.
- Keep this file up to date: whenever project instructions change, update `AGENTS.md` in the same change.

## Local Services
- Use `docker compose` with `docker/docker-compose.yml` for local Postgres/pgAdmin when needed.

## Code Conventions
- Keep package base: `vibe.webapp`.
- Use Spring Data repositories for data access.
- Use JPA entities with Hibernate.

## Security
- Keep RBAC with roles `ROLE_ADMIN` and `ROLE_USER`.
- Use form login + HTTP Basic as currently configured unless asked to change.

## Testing
- Unit tests end with `*Test` and run with `./mvnw test`.
- Integration tests end with `*IT` and run with `./mvnw -Pintegration-test verify`.

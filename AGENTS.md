# Agent Instructions

## Project Basics

The project purpose is to serve as a simple electronic parts tracker
for a local hobby electronic club. We don't expect a huge traffic here,
but the app should be able to handle 15 concurrent users.

The main usecase is to catalog vast amount of electronic parts (10k+ parts).
Each part needs to be associated with description, links to other blogs and websites
showcaseing the part application, mandatory single photo.
Each part has an inventory count (how many parts are left).
Users can borrow parts for their projects, club can buy new parts. As it often happens parts get
destroyed. We need to provide an inventory tracker for this.
Additionaly we need to support advanced tagging and tree-like hierarchy organization for
easy browsing.
Users should be able to leave both comments and ratings for each part.
In the future we will want to add a machine learning recommendation system.
Searching is the heart of the system, thats why we decided to use a full text search solution.
Usually users don't know the part numbers they use a descriptions like "arduino camera module"
or "amplifier 5V". We need to help them see the parts that are currently available even when they
use vague descriptions (hence the hierarchy and tagging support in addition to a full-text search).

The app will be exposed over the internet, the security and user privacy are important.
Especially we should not allow a situation when we have one of the OWASP Top 10 bugs in our app.

If possible the app should be able to run on a low cost SBC like Raspberry PI 4 or 5.
The application code is published on GitHub, our codebase should follow best technical
and community standards (let's be a good open source community memeber).
The application architecture is crucial for future maintanance. We should only use prooven and
working pattern here like a modulith or 3-layer architecture.

## Technical details

- Project: `parts-vibe`
- Language: Java 25
- Build tool: Maven
- Framework: Spring Boot + Thymeleaf
- Database: PostgreSQL
- Full text search: Apache Solr
- Modules: `webapp`, `shared`, `data-access`, `application`, `search` (multi-module Maven)

## Command Rules
- Always use the Maven Wrapper for Maven-related commands: `./mvnw` (not `mvn`).
- To save tokens try to use `./mvnw -q` as often as possible (you may switch to more verbose mode to solve problems).
- Do not run Maven commands unless asked.

## Local Services
- Use `docker compose` with `docker-compose/docker-compose.yml` for local Postgres/pgAdmin/Solr/Prometheus/Grafana/Loki when needed.
- Ask before running this command.

## Code Conventions
- Use Spring Data repositories for data access.
- Use JPA entities with Hibernate.
- UI templates use Bootstrap 5 via WebJars (no locator).

## Security
- The applicaiton uses Spring Security RBAC-model.
- When posting a form always use POST-redirect-GET pattern to preven double-POSTing of forms.

## Testing
- Unit tests end with `*Test` and run with `./mvnw test`.
- Integration tests end with `*IT` and run with `./mvnw -Pintegration-test verify`.

## Extra

See `README.md` for full project usage, services, and environment details.

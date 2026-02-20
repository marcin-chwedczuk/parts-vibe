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
- Modules: `app`, `shared`, `catalog`, `users`, `ui-components`, `search-api`, `search` (multi-module Maven)

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
- Custom application/domain exceptions should extend `ApplicationException` (`app.partsvibe.shared.error.ApplicationException`).

## Local IDE Files
- Ignore changes under `.idea/` caused by IDE filesystem watchers; do not treat them as unexpected workspace modifications.

## Security
- The applicaiton uses Spring Security RBAC-model.
- When posting a form always use POST-redirect-GET pattern to preven double-POSTing of forms.

## Testing
- Unit tests end with `*Test` and run with `./mvnw test`.
- Integration tests end with `*IT` and run with `./mvnw -Pintegration-test verify`.

## Extra

See `README.md` for full project usage, services, and environment details.

## Architecture Decisions (Living)

This section captures project decisions made during implementation sessions.
Treat it as the default unless explicitly overridden.

### Build / Version Metadata
- Build metadata is generated during Maven build and filtered into a dedicated properties file.
- Use Spring-style resource filtering delimiters (`@...@`) and filter only the intended file (not all resources).
- Git metadata resolution in submodules requires correct `.git` path configuration (`maven.multiModuleProjectDirectory`).
- Expose application version in UI footer using git tag-derived value (`v...` expected).

### Maven / Dev Workflow
- Keep Maven usage via wrapper only (`./mvnw`).
- SpotBugs can be skipped during active development via profile (`skip-spotbugs`).
- User runs tests manually in many flows; do not auto-run Maven unless explicitly requested.

### Module Boundaries
- The project is a modulith intended for future microservice split.
- App-wide infrastructure/configuration belongs in `app` (e.g. security/bootstrap), not feature modules.
- `shared` holds cross-module contracts and base abstractions.
- `infra` holds implementations/adapters.

### CQRS
- CQRS contracts live in `app.partsvibe.shared.cqrs`.
- Spring mediator implementation lives in `infra` and resolves handlers by generic type.
- Controllers should use `Mediator` instead of direct service interfaces.
- Command/query handlers can use shared base classes:
  - `BaseCommandHandler` with transactional boundary (`REQUIRED`)
  - `BaseQueryHandler` with read-only transactional boundary (`REQUIRED`, `readOnly=true`)
- Pipeline/decorator style behaviors are supported (`CommandBehavior`, `QueryBehavior`).
- Command validation is implemented as a high-priority behavior using Bean Validation.
- Use `NoResult` when command returns no business value.

### Events / Event Queue
- Naming uses `event queue` terminology (avoid `outbox/inbox` naming drift).
- Event handling is handler-mapping based, not event-class-only mapping.
- Event handlers must declare handled events explicitly using `@HandlesEvent(name, version)` (repeatable).
- Multiple handlers for the same `(event name, version)` are supported.
- `EventHandler` contract requires idempotency (at-least-once delivery).
- Event queue consumer deserializes payload separately per handler invocation to isolate mutable side effects.
- RequestId scope is set per handler call.
- Event handler discovery/registry is Spring-based:
  - `EventHandlerRegistry`
  - `SpringEventHandlerRegistry`
- Dispatcher/consumer responsibilities are separated:
  - Dispatcher: claiming/timeout/DB state transitions
  - Consumer: deserialization + invoking handlers

### Event Metrics / Logging
- Event queue metric prefix convention: `app.event-queue.*`.
- Focus metrics:
  - claimed count
  - done count
  - failed count
  - timed-out count
  - lag and processing durations
- Logging conventions:
  - use placeholders (`{}`), no string concatenation in log templates
  - include event id/name/version and handler identity in failures

### RequestId
- RequestId propagation is not MDC-only.
- Provider uses thread-scoped storage and supports scoped usage (`withRequestId(...)`).
- Event processing restores RequestId from event payload for handler execution.

### JPA / Hibernate
- Shared base hierarchy introduced in `shared.persistence`:
  - `BaseEntity`
  - `BaseVersionableEntity`
  - `BaseAuditableEntity`
- Equality/hashCode for entities follow Hibernate-safe pattern based on effective class + non-null id.
- Sequence strategy:
  - Base id uses sequence generator name constant.
  - Concrete entities define `@SequenceGenerator` with project standard allocation size (`50`).
- Current entity inheritance policy:
  - Most entities use `BaseAuditableEntity`.
  - `EventQueueEntry` currently uses `BaseEntity` (audit intentionally deferred for queue row).
- Auditing:
  - Enabled via `@EnableJpaAuditing` in infra.
  - `AuditorAware<String>` uses authenticated principal when available; fallback is `"system"`.
- For JPA entities:
  - use `@NoArgsConstructor(access = PROTECTED)`.
  - avoid `@Data`; use targeted Lombok annotations.
  - avoid public setter replacing entire persistent collections.
  - for collection fields (e.g. `User.roles`), disable replacement setter (`@Setter(AccessLevel.NONE)`).

### QueryDSL / Read Grids
- QueryDSL is used for read paths (CQRS query handlers), not for controllers/domain logic directly.
- Controller read flow should call `Mediator.executeQuery(...)`; avoid in-controller filtering/paging over in-memory lists.
- Query contracts/results for user-management grid live in `users` module (`users.queries.usermanagement`).
- Prefer domain/intent names in backend code; avoid UI-specific names like `Grid`, `Table`, or `Management` in query/filter/handler type names.
  - Current convention examples: `SearchUsersQuery`, `SearchUsersQueryHandler`, `UserFilters`, `UserWebMapper`.
- Parent `pom.xml` owns QueryDSL version/dependency management (`querydsl-jpa`, `querydsl-apt`, jakarta classifier).
- Modules using QueryDSL must configure annotation processing in `maven-compiler-plugin`.
- `shared` module must also generate QueryDSL metadata for mapped base entities (`QBase*`) used by feature modules.
- Do not hardcode `jakarta.persistence-api` versions in module POMs; rely on managed dependency versions.
- Sorting and paging inputs must be allowlisted/sanitized; never map raw request sort params directly to query expressions.
- For user-management filters, generic page/sort sanitization lives in `UserFilters`; role allowlist validation remains in `UsersController`.
- CQRS pagination contract:
  - `PaginatedQuery` exposes `currentPage` and `pageSize` (avoid short names like `page`/`size`).
  - `BasePaginatedQueryHandler` centralizes page normalization and clamping.
  - `PaginationPolicy` is the single source for pagination defaults/limits (`DEFAULT_PAGE_SIZE=10`, `ALLOWED_PAGE_SIZES=10,25,50`, `MAX_PAGE_SIZE=50`).
- `JPAQueryFactory` should be provided as a singleton Spring bean and injected into query handlers (do not instantiate per handler).
- Users search query currently keeps a TODO to replace roles mapping with a paged-id + batched projection approach to avoid potential N+1 on roles.
- Preserve PRG state using typed filter fields and hidden inputs/fragments; avoid free-form `returnUrl` redirects.

### Lombok
- Root `lombok.config` is required.
- Preserve Spring DI parameter annotations on Lombok-generated constructors:
  - `@Qualifier`, `@Value`, `@Autowired`, `@Lazy`, `@Named`.
- Add `lombok.addLombokGeneratedAnnotation = true`.
- Keep guardrail `lombok.data.flagUsage = warning`.

### Security / Web
- Maintain Spring Security RBAC model.
- Use PRG (`POST -> redirect -> GET`) for forms.
- Static assets (e.g. logos) must be explicitly allowed by security config.
- Default app entry (`/`) redirects to catalog search (`/catalog/search`).
- Everything under `/admin/**` must be accessible only to `ROLE_ADMIN` (keep URL-based rules and method-level checks aligned).

### Frontend / Thymeleaf Layout
- Static resources follow `static/resources/*` subdivision (`css`, `images`, `fonts`, `js`).
- SCSS is compiled during normal Maven build using `frontend-maven-plugin` (Dart Sass), with generated CSS written to `app/target/generated-resources/...` (not to `src/main/resources`).
- Bootstrap Icons are provided via WebJars (`org.webjars.npm:bootstrap-icons`) and loaded in base layout.
- Base shell uses sticky-footer layout:
  - `body`: `d-flex flex-column min-vh-100`
  - `main`: `flex-grow-1`
- Sidebar pages use role-specific fragments (`layout/user`, `layout/admin`) and a bordered content pane; sidebar remains borderless between navbar and footer.
- Thymeleaf rule: do not put `th:replace` on elements that carry critical layout classes (flex/spacing/border wrappers), because replacement removes that element and its classes.
- Prefer:
  - keep structural wrapper in DOM
  - use inner `th:replace` / `th:insert` for fragment content.

### Thymeleaf UI Components / Dialect
- Reusable UI tags live in the `ui-components` module.
- Custom tag prefix is `app` (for example: `<app:pagination .../>`, `<app:confirmation-dialog .../>`).
- Prefer a single `app:data` attribute bound to a typed contract interface, not many scalar attributes.
  - Example contracts:
    - `PaginationModel`, `PaginationLinkModel`
    - `ConfirmationDialogModel`
- Keep component structure in template files under `ui-components/src/main/resources/templates/ui/components/*`.
  - Java processor should stay thin: validate input, set local vars, parse template, merge slots.
- For rich/customizable content use named slots (`app:slot`) with fallback content in component templates.
  - Confirmation dialog slots: `title`, `body`, `fields`, `confirm`, `cancel`.
- Security defaults for confirmation dialog:
  - form method must be `POST` (reject others in processor validation).
  - pass CSRF and extra hidden fields via `fields` slot.
- Avoid placing `th:each` directly on custom dialect elements; wrap iteration in outer `th:block` and place custom tag inside.
- If browser shows `ERR_INCOMPLETE_CHUNKED_ENCODING` on a Thymeleaf page, suspect template rendering failure (often custom processor/slot merge errors) and check server logs.
- Component tests should render real templates via Thymeleaf engine and assert DOM with Jsoup/CSS selectors.
  - For error-path tests, assert on `TemplateInputException` with root cause `TemplateProcessingException`.
- Keep XSD for custom namespace under `ui-components/src/main/resources/META-INF/*.xsd` and include `xs:documentation` for element/attribute usage.

### Local Infrastructure
- Docker compose setup includes observability stack and optional runtime components.
- Antivirus integration uses ClamAV (`clamd`) scanning path in infra.

# Copilot Instructions for helse-spesialist

## What is this?

Backend for saksbehandling av sykepengesøknader (adjudication of sick pay applications) at NAV. Built with Kotlin, Ktor, and PostgreSQL on NAV's Rapids & Rivers event streaming architecture.

## Build & Test

```shell
# Build everything
./gradlew build

# Run all tests
./gradlew test

# Run tests for a single module
./gradlew :spesialist-domain:test
./gradlew :spesialist-db:test

# Run a single test class
./gradlew :spesialist-domain:test --tests "no.nav.helse.spesialist.domain.BehandlingTest"

# Run a single test method
./gradlew :spesialist-domain:test --tests "no.nav.helse.spesialist.domain.BehandlingTest.someTestMethod"

# Lint (ktlint)
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

Docker (or Colima) is required for tests — database tests use Testcontainers with PostgreSQL.

## Architecture

Onion architecture with strict dependency direction — outer layers depend inward only, never sideways or outward.

```
spesialist-bootstrap          ← App entry point, wires all modules together
  ├── spesialist-api           ← REST (Ktor + OpenAPI) and GraphQL (graphql-kotlin) endpoints
  │     └── spesialist-api-schema  ← GraphQL schema definitions, REST resource types
  ├── spesialist-kafka         ← Rapids & Rivers message handlers (40+ rivers)
  ├── spesialist-db            ← PostgreSQL DAOs (Kotliquery), Flyway migrations
  ├── spesialist-client-*      ← HTTP clients for external services (Spleis, KRR, Entra ID)
  └── spesialist-application   ← Commands, use cases, orchestration
        └── spesialist-domain  ← Pure domain model, no infrastructure dependencies
```

## Key Conventions

### Domain-Driven Design (spesialist-domain)

- DDD base classes in `domain/ddd/`: `AggregateRoot<T>`, `Entity<T>`, `ValueObject`, `LateIdAggregateRoot`
- Type-safe IDs using `@JvmInline value class` (e.g., `Identitetsnummer`, `VedtaksperiodeId`, `NAVIdent`)
- Sealed classes for domain type hierarchies (`Vedtak`, `Overstyring`, `PåVent`)
- Domain events as data classes implementing sealed interfaces (e.g., `TilkommenInntektEvent`)

### Command Pattern (spesialist-application)

- `Command` interface with `execute()` and optional `resume()`
- `MacroCommand` for composing sequential command chains
- `CommandContext` carries shared state through command execution

### Rivers / Kafka (spesialist-kafka)

- Each message type has its own River class implementing `SpesialistRiver` (sealed interface)
- Two variants: `TransaksjonellRiver` (database transaction-wrapped) and plain `SpesialistRiver`
- Messages use the Rapids & Rivers `JsonMessage` format with `@id`, `@event_name`, and domain fields
- Messages are persisted as JSON before processing (event sourcing-lite)

### API Layer (spesialist-api)

- **Migrering fra GraphQL til REST pågår** — nye endepunkter skal lages som REST, ikke GraphQL
- REST: Ktor typed routing with `@Resource` annotations and `RestBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>` handler interfaces
- GraphQL (utfases): Query and Mutation handler classes (e.g., `PersonQueryHandler`, `TildelingMutationHandler`)
- JWT authentication via Nimbus JOSE

### Database (spesialist-db)

- Raw SQL via Kotliquery (no ORM) — use `@Language("SQL")` annotation on SQL strings
- DAOs extend `HelseDao` base class with `asSQL()`, `insert()`, `list()`, `single()` helpers
- `somDbArray()` converts collections to PostgreSQL `{}` array syntax
- Migrations managed by Flyway in `spesialist-db-migrations`
- `TransactionalSessionFactory` for transaction management

### Testing

- **Domain tests**: Plain JUnit 5, no DB required. Use test fixtures (`lagBehandling()`, etc.)
- **DB tests**: Extend `AbstractDBIntegrationTest` — provides Testcontainers PostgreSQL, auto-migration, and table truncation between tests
- **E2E tests**: Two patterns — newer `AbstractE2EIntegrationTest` (full app bootstrap with stubs) and legacy `AbstractE2ETest`
- Test fixtures are shared via Gradle's `java-test-fixtures` plugin

### Language & Naming

- Code and comments are in Norwegian (domain terms: `vedtaksperiode`, `saksbehandler`, `godkjenning`, `oppgave`, `utbetaling`)
- Package structure: `no.nav.helse.spesialist.*` for new code, `no.nav.helse.*` for legacy

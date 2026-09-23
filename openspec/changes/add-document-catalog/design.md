# Design

## Context

The repository contains OpenSpec configuration but no application code, build, database schema, or existing capabilities. The behavior contract is defined in `specs/document-catalog/spec.md`; this design establishes a small enterprise-style implementation that remains understandable as a learning lab.

The service owns only document metadata. Producing systems store document files elsewhere and supply an opaque storage reference. Consumers are internal applications, but authentication and authorization are deliberately excluded from the initial capability.

## Goals / Non-Goals

**Goals:**

- Make the path from specification scenarios to implementation and automated tests easy to trace.
- Separate HTTP, application, domain, and persistence responsibilities without introducing a full architecture framework.
- Use production-representative PostgreSQL behavior for development migrations and integration tests.
- Keep API and database contracts explicit and independent of incidental Spring serialization behavior.

**Non-Goals:**

- Introduce generic ports, adapters, command buses, events, or multiple deployable modules.
- Contact the external document store or verify storage references.
- Add production deployment manifests, authentication, authorization, or advanced observability.
- Support mutation, deletion, idempotent registration, or arbitrary search.

## Decisions

### Use Java 21, Spring Boot, and a single Maven module

The application will be a single executable Spring Boot service built with Maven and Java 21. Dependencies will include Spring Web, Bean Validation, Spring Data JPA, PostgreSQL, Flyway, and the standard Spring Boot test support, plus PostgreSQL Testcontainers for integration tests.

A single module keeps build and navigation costs low. A multi-module build was rejected because there is only one bounded capability and no independently reusable component.

### Organize packages by feature with explicit internal layers

The root package will contain a `document` feature with `api`, `application`, `domain`, and `persistence` subpackages, plus narrowly scoped common error and configuration packages.

```text
documentcatalog
|
+-- document
|   +-- api
|   +-- application
|   +-- domain
|   +-- persistence
|
+-- common
    +-- error
    +-- configuration
```

Controllers own HTTP translation, an application service coordinates transactions and use cases, domain types express catalog concepts, and persistence classes own JPA concerns. This provides visible boundaries without the ceremony of a full hexagonal architecture.

### Separate API DTOs, domain objects, and JPA entities

Request and response DTOs will define the REST contract. The domain representation will model document metadata and document types. A separate JPA entity will define relational mapping. Explicit mapping code will connect the three representations.

Using one annotated class everywhere would reduce code, but it would couple JSON, domain, and database changes and weaken the lab's architectural lesson. A generic mapping library is unnecessary for this small model and would obscure the transformations.

### Generate catalog identity and ingestion time in the application

The application service will assign a UUID and obtain `recordedAt` from an injected clock before persistence. Injecting the clock makes timestamp behavior deterministic in tests. Timestamps will be stored as UTC-capable values and serialized in ISO-8601 form.

Database-generated identity was rejected because application generation makes the complete domain object available before persistence and avoids database-specific ID behavior.

### Normalize inputs once at the API boundary

Registration DTO validation will enforce presence, lengths, supported enum values, and positive size. Mapping into the application command will trim surrounding whitespace from string fields while preserving case. The normalized source pair is used for both duplicate checks and persistence.

Jackson will be configured to reject unknown JSON properties. MIME type and storage reference remain bounded strings rather than parsed or dereferenced values because the catalog does not own either external contract.

### Use PostgreSQL with Flyway-owned schema

Flyway will create a single `document_metadata` table. Hibernate schema generation will validate mappings rather than create or update tables. The table will store UUID identity, normalized source fields, customer ID, document type as text, filename, content type, positive byte size, opaque storage reference, document date, and recorded timestamp.

The schema will include:

- A primary key on `id`.
- A unique constraint on `(source_system, source_document_id)`.
- An index supporting customer/type/date listing.
- Check constraints where they reliably mirror invariant scalar rules such as positive size.

Document types will be stored as strings in a character column rather than ordinal values or a PostgreSQL enum. This keeps values readable and lets later OpenSpec changes add types without database enum migrations.

H2 was rejected because differences in SQL, constraints, and types can make tests pass against behavior that differs from PostgreSQL.

### Make the database constraint authoritative for duplicates

Registration will run in a single application-service transaction. The implementation may perform an early repository existence check for a clear common-case failure, but it must translate the unique-constraint violation to the same `409 Conflict` response. This preserves correctness under concurrent registrations.

Idempotent comparison and returning an existing record are deliberately not included. They can be introduced later as a separate specification evolution.

### Define explicit REST representations

The controller will expose only:

- `POST /api/v1/documents`
- `GET /api/v1/documents/{documentId}`
- `GET /api/v1/documents`

The list endpoint will construct an explicit page response containing `items`, `page`, `size`, `totalElements`, and `totalPages`. It will not return Spring Data's `Page` directly, keeping the external contract stable across framework changes.

Sorting will be fixed to `documentDate DESC, id DESC`; clients will not supply arbitrary sort expressions in the initial capability.

### Centralize Problem Details translation

A global exception handler will translate validation, malformed input, not-found, duplicate, and unexpected errors into `ProblemDetail` responses. Validation failures will add structured `fieldErrors`. Stable application error details will be exposed while exception names, stack traces, SQL, and database messages remain internal.

### Test each architectural boundary at the appropriate level

JUnit 5 tests will be divided into:

- Unit tests for domain normalization/rules and application-service orchestration using an injected clock.
- PostgreSQL Testcontainers repository tests that run Flyway migrations and verify constraints, mapping, filtering, and ordering.
- HTTP integration tests for JSON contracts, status codes, headers, validation, Problem Details, pagination, and filtering.
- A small end-to-end happy path through HTTP and PostgreSQL.

Integration tests will use PostgreSQL rather than substituting H2. Tests will trace directly to specification scenarios without duplicating low-value framework behavior.

## Risks / Trade-offs

- [Separate API, domain, and persistence models add mapping code] -> Keep mappings explicit and colocated with the document feature; do not add a mapping framework.
- [A producer could register a storage reference that does not exist] -> Preserve the bounded context and document that the producing system owns storage validity.
- [An early duplicate lookup cannot prevent registration races] -> Enforce the database unique constraint and translate its violation consistently.
- [Offset pagination can become inefficient for very large customer histories] -> Accept it for the learning-sized dataset and reserve cursor pagination for a later change.
- [Fixed enum values require a code release to add a document type] -> Treat type expansion as an intentional OpenSpec evolution and persist names to keep migration simple.
- [Testcontainers requires a working container runtime] -> Document the prerequisite and fail integration tests clearly rather than silently falling back to a different database.

## Migration Plan

1. Introduce the Maven/Spring Boot application and Flyway baseline migration together.
2. Start PostgreSQL, then start the application so Flyway creates the catalog table before JPA validates mappings.
3. Verify registration, retrieval, listing, and error behavior through automated tests.

This is a greenfield service with no existing data migration. Before the service is used, rollback consists of reverting the application and dropping the lab database or schema. Once data exists, destructive rollback requires preserving the table and deploying the prior compatible application rather than reversing the migration automatically.

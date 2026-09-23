# Proposal

## Why

Internal applications need a small, well-defined catalog for recording and finding metadata about documents whose files are stored by other systems. Building this capability as a focused Spring Boot application provides a practical lab for learning the OpenSpec spec-driven workflow from behavioral requirements through implementation and verification.

## What Changes

- Add registration of metadata for statements, letters, notices, and customer-uploaded documents without accepting or storing document content.
- Add retrieval of one metadata record by its catalog-assigned UUID.
- Add paginated listing of a customer's document metadata with optional document-type filtering and deterministic newest-first ordering.
- Reject invalid registration and query inputs through a consistent Problem Details error contract.
- Reject duplicate `(sourceSystem, sourceDocumentId)` registrations with `409 Conflict`.
- Establish metadata as immutable in the initial capability; updating, deleting, and idempotently registering metadata remain out of scope.
- Add a Java 21 Spring Boot service backed by PostgreSQL with explicit Flyway migrations and automated tests using JUnit 5 and Testcontainers.

## Capabilities

### New Capabilities

- `document-catalog`: Register, retrieve, and list immutable metadata for externally stored customer documents.

### Modified Capabilities

None.

## Impact

- Introduces the initial Maven and Spring Boot application structure in this otherwise greenfield repository.
- Adds a versioned REST API under `/api/v1/documents` for internal producers and consumers.
- Adds a PostgreSQL schema owned through Flyway migrations.
- Adds Spring Web, Validation, Data JPA, PostgreSQL, Flyway, JUnit 5, and Testcontainers dependencies.
- Depends on producing systems to supply metadata and an opaque storage reference; it does not contact or validate the external document store.

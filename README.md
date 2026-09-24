# OpenSpec Java Lab

A learning project for exploring spec-driven development using OpenSpec, Java, Spring Boot, and AI coding agents.

## Prerequisites

- Java 21
- A Docker-compatible container runtime for PostgreSQL and Testcontainers
- The OpenSpec CLI for the specification workflow

Maven does not need to be installed globally. The checked-in `./mvnw` script downloads the pinned Maven distribution into the normal Maven user cache on first use.

On macOS, Docker Desktop works without additional environment variables. When using Colima, start it and expose its socket to Testcontainers:

```bash
colima start --runtime docker
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
```

## Build and test

Run the complete unit and PostgreSQL Testcontainers suite:

```bash
./mvnw clean verify
```

Flyway applies `src/main/resources/db/migration/V1__create_document_metadata.sql` to every empty test database. Hibernate uses `ddl-auto: validate`, so the application checks its mappings but never creates or updates the schema itself.

## Run locally

Start a PostgreSQL 17 database with the defaults from `application.yml`:

```bash
docker run --name document-catalog-postgres \
  -e POSTGRES_DB=document_catalog \
  -e POSTGRES_USER=document_catalog \
  -e POSTGRES_PASSWORD=document_catalog \
  -p 5432:5432 \
  -d postgres:17-alpine
```

Then start the service:

```bash
./mvnw spring-boot:run
```

Configuration can be overridden with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. At startup, Flyway creates or upgrades the schema before Hibernate validates the mappings.

The initial API contains only:

```text
POST /api/v1/documents
GET  /api/v1/documents/{documentId}
GET  /api/v1/documents?customerId=...&documentType=...&page=0&size=20
```

## OpenSpec learning workflow

The repository keeps the reasoning and behavioral contract beside the code:

1. Explore requirements without implementation using `openspec-explore`.
2. Review the change artifacts under `openspec/changes/add-document-catalog/`.
3. Inspect progress with `openspec instructions apply --change add-document-catalog --json`.
4. Implement and verify each checkbox in `tasks.md` using `openspec-apply-change`.
5. After implementation review, archive the change using `openspec-archive-change` so its delta spec becomes a durable project specification.

The application intentionally excludes file upload/download, authentication, update/delete operations, and idempotent registration. Those are candidates for later changes that demonstrate specification evolution.

## Specification traceability

The initial capability is specified in `openspec/changes/add-document-catalog/specs/document-catalog/spec.md`. Each scenario maps to executable coverage:

- Scenario: **Register valid statement metadata** — `DocumentRegistrationHttpTest`, `DocumentCatalogEndToEndTest`
- Scenario: **Register future-dated metadata** — `DocumentMetadataTest`, `DocumentRegistrationHttpTest`
- Scenario: **Preserve opaque storage reference** — `DocumentCatalogServiceRegistrationTest`, `DocumentCatalogEndToEndTest`
- Scenario: **Register each supported document type** — `DocumentMetadataTest`, `DocumentRegistrationHttpTest`
- Scenario: **Reject an unsupported document type** — `DocumentApiDtoTest`, `ApiErrorContractTest`
- Scenario: **Reject a missing required field** — `ApiErrorContractTest`
- Scenario: **Reject a blank string field** — `DocumentMetadataTest`, `ApiErrorContractTest`
- Scenario: **Normalize surrounding whitespace** — `DocumentMetadataTest`, `DocumentApiDtoTest`, `DocumentRegistrationHttpTest`
- Scenario: **Reject an oversized string field** — `DocumentMetadataTest`, `DocumentApiDtoTest`
- Scenario: **Reject a non-positive document size** — `DocumentMetadataTest`, `DatabaseMigrationTest`, `ApiErrorContractTest`
- Scenario: **Reject an unknown JSON property** — `DocumentApiDtoTest`, `ApiErrorContractTest`
- Scenario: **Reject an existing source identity** — `DocumentMetadataRepositoryTest`, `DuplicateRegistrationIntegrationTest`, `ApiErrorContractTest`
- Scenario: **Allow source identifiers that differ by case** — `DatabaseMigrationTest`, `DocumentMetadataRepositoryTest`
- Scenario: **Resolve a concurrent duplicate registration** — `DuplicateRegistrationIntegrationTest`
- Scenario: **Retrieve an existing document** — `DocumentCatalogServiceRetrievalTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **Retrieve an unknown document** — `DocumentCatalogServiceRetrievalTest`, `DocumentRetrievalAndListingHttpTest`, `ApiErrorContractTest`
- Scenario: **Reject a malformed document ID** — `DocumentRetrievalAndListingHttpTest`
- Scenario: **List a customer's documents** — `DocumentMetadataRepositoryTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **Filter a customer's documents by type** — `DocumentMetadataRepositoryTest`, `DocumentCatalogServiceListingTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **List a customer with no documents** — `DocumentCatalogServiceListingTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **Use default pagination** — `DocumentCatalogServiceListingTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **Request a specific valid page** — `DocumentCatalogServiceListingTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **Reject invalid pagination** — `DocumentCatalogServiceListingTest`, `DocumentRetrievalAndListingHttpTest`
- Scenario: **Reject a missing customer ID** — `DocumentRetrievalAndListingHttpTest`
- Scenario: **Return validation problem details** — `ApiErrorContractTest`
- Scenario: **Return conflict problem details** — `ApiErrorContractTest`
- Scenario: **Return unexpected failure problem details** — `ApiErrorContractTest`
- Scenario: **Use the documented API surface** — `DocumentApiSurfaceTest`

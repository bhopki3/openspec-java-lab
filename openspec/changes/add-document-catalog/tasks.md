# Tasks

## 1. Application Foundation

- [ ] 1.1 Create the Java 21 single-module Maven Spring Boot application with Web, Validation, Data JPA, PostgreSQL, Flyway, JUnit 5, and Testcontainers dependencies, and verify `./mvnw test` starts from a clean checkout.
- [ ] 1.2 Add application configuration for PostgreSQL, Flyway migrations, JPA schema validation, UTC serialization, and unknown-JSON-property rejection, and verify an application context test loads against PostgreSQL Testcontainers.
- [ ] 1.3 Establish the feature-oriented `document` and `common` package structure plus an injectable UTC `Clock`, and verify architecture/package tests prevent API, domain, and persistence responsibilities from collapsing together.

## 2. Domain and Persistence

- [ ] 2.1 Implement the document metadata domain model and named `DocumentType` values, including input normalization and invariants, and verify unit tests cover trimming, case preservation, positive size, all supported types, and future document dates.
- [ ] 2.2 Add the Flyway baseline migration for `document_metadata` with aligned column lengths, UUID primary key, positive-size check, case-sensitive source-identity uniqueness, and customer/type/date index, and verify the migration succeeds on PostgreSQL Testcontainers.
- [ ] 2.3 Implement the separate JPA entity, explicit domain/entity mapper, and Spring Data repository queries for identity checks, lookup, and deterministic customer listing, and verify repository integration tests cover round trips, case sensitivity, constraints, filtering, pagination, and `documentDate DESC, id DESC` ordering.

## 3. Application Use Cases

- [ ] 3.1 Implement transactional registration with application-generated UUID and clock-derived `recordedAt`, preserving the opaque storage reference, and verify service tests cover successful registration and assigned fields.
- [ ] 3.2 Implement duplicate source-identity handling backed by the database unique constraint, translating both ordinary and concurrent duplicate attempts to a catalog conflict, and verify integration tests create exactly one record and observe conflict outcomes for the rest.
- [ ] 3.3 Implement retrieval by catalog UUID with a not-found outcome, and verify service tests cover existing and unknown IDs.
- [ ] 3.4 Implement customer listing with optional document-type filtering, fixed sorting, zero-based pagination, defaults of page 0 and size 20, and maximum size 100, and verify service tests cover filtered, empty, default, and requested pages.

## 4. REST API and Errors

- [ ] 4.1 Implement separate registration request, metadata response, and page response DTOs with explicit mapping and aligned Bean Validation constraints, and verify serialization and validation tests cover every field, length boundary, non-positive size, supported types, trimming, and unknown properties.
- [ ] 4.2 Implement `POST /api/v1/documents` returning `201 Created`, the created representation, and a resource `Location` header, and verify HTTP integration tests cover valid registration, future dates, opaque storage references, and every supported document type.
- [ ] 4.3 Implement `GET /api/v1/documents/{documentId}` and `GET /api/v1/documents` with the specified query contract and custom pagination representation, and verify HTTP integration tests cover retrieval, malformed and unknown IDs, customer isolation, filtering, ordering, empty results, defaults, and invalid query parameters.
- [ ] 4.4 Implement centralized Spring `ProblemDetail` handling for validation, malformed input, not-found, duplicate, and unexpected failures with structured `fieldErrors`, and verify contract tests assert `type`, `title`, `status`, and `detail` while ensuring responses omit exception, SQL, stack-trace, and database details.
- [ ] 4.5 Ensure the controller exposes no update or delete operation, and verify API-level tests or architecture assertions limit the document API to registration, retrieval, and listing.

## 5. End-to-End Verification and Learning Documentation

- [ ] 5.1 Add a full HTTP-to-PostgreSQL happy-path test that registers, retrieves, and lists a document, and verify the returned representations and persisted values match the specification.
- [ ] 5.2 Add a specification traceability section to the project documentation mapping each OpenSpec requirement to its automated test coverage, and verify every scenario in the document-catalog delta spec is represented.
- [ ] 5.3 Document local PostgreSQL/container-runtime prerequisites, configuration, migration behavior, and commands for starting and testing the lab, and verify a learner can follow the documented commands from a clean checkout.
- [ ] 5.4 Run the complete Maven verification lifecycle and strict OpenSpec validation, and verify all unit, repository, HTTP, end-to-end, migration, and specification checks pass before marking the change implemented.

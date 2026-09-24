# Spec Delta

## Purpose

Provide internal applications with a consistent catalog for registering and finding metadata about customer documents stored by external systems.

## ADDED Requirements

### Requirement: Register document metadata
The system SHALL accept `POST /api/v1/documents` requests containing `sourceSystem`, `sourceDocumentId`, `customerId`, `documentType`, `filename`, `contentType`, `sizeBytes`, `storageReference`, and `documentDate`. On successful registration, the system SHALL assign a UUID `id`, assign a UTC `recordedAt` timestamp, persist the metadata without document content, and return `201 Created` with the created representation and a `Location` header identifying `/api/v1/documents/{id}`.

#### Scenario: Register valid statement metadata
- **WHEN** a producer submits complete, valid metadata for a statement whose source identity has not been registered
- **THEN** the system stores the metadata and returns `201 Created` with the assigned `id`, assigned `recordedAt`, submitted metadata, and resource location

#### Scenario: Register future-dated metadata
- **WHEN** a producer submits otherwise valid metadata with a future `documentDate`
- **THEN** the system accepts the registration without changing the supplied `documentDate`

#### Scenario: Preserve opaque storage reference
- **WHEN** a producer registers a document with a nonblank `storageReference`
- **THEN** the system stores and returns that reference unchanged and does not dereference or validate it against an external storage system

### Requirement: Support defined document types
The system SHALL accept exactly `STATEMENT`, `LETTER`, `NOTICE`, and `CUSTOMER_UPLOAD` as document types and SHALL persist document-type values by name.

#### Scenario: Register each supported document type
- **WHEN** a producer submits otherwise valid metadata using any defined document type
- **THEN** the system accepts and returns that document type

#### Scenario: Reject an unsupported document type
- **WHEN** a producer submits a document type outside the defined set
- **THEN** the system returns `400 Bad Request` using the Problem Details error contract

### Requirement: Validate registration input
The system SHALL require every registration field. It SHALL trim surrounding whitespace from string inputs, preserve their remaining casing, require nonblank strings, require `sizeBytes` to be greater than zero, and enforce maximum lengths of 100 characters for `sourceSystem`, 200 for `sourceDocumentId`, 100 for `customerId`, 255 for `filename`, 100 for `contentType`, and 500 for `storageReference`. The system SHALL reject unknown JSON properties.

#### Scenario: Reject a missing required field
- **WHEN** a registration request omits any required field
- **THEN** the system returns `400 Bad Request` with a field error identifying the missing field

#### Scenario: Reject a blank string field
- **WHEN** a registration request supplies only whitespace for a required string field
- **THEN** the system returns `400 Bad Request` with a field error identifying that field

#### Scenario: Normalize surrounding whitespace
- **WHEN** a registration request supplies a valid string value with surrounding whitespace
- **THEN** the system stores the trimmed value while preserving its casing

#### Scenario: Reject an oversized string field
- **WHEN** a registration request supplies a string longer than its defined maximum
- **THEN** the system returns `400 Bad Request` with a field error identifying that field

#### Scenario: Reject a non-positive document size
- **WHEN** a registration request supplies `sizeBytes` less than or equal to zero
- **THEN** the system returns `400 Bad Request` with a field error identifying `sizeBytes`

#### Scenario: Reject an unknown JSON property
- **WHEN** a registration request contains a property not defined by the registration contract
- **THEN** the system returns `400 Bad Request` using the Problem Details error contract

### Requirement: Enforce unique source identity
The system SHALL treat the trimmed `(sourceSystem, sourceDocumentId)` pair as a case-sensitive source identity and SHALL permit only one metadata record for each pair. Duplicate detection SHALL remain correct when concurrent requests attempt to register the same source identity.

#### Scenario: Reject an existing source identity
- **WHEN** a producer registers metadata whose case-sensitive source identity already exists
- **THEN** the system returns `409 Conflict` and does not create another record

#### Scenario: Allow source identifiers that differ by case
- **WHEN** a producer registers metadata whose source identity differs from an existing identity only by letter case
- **THEN** the system creates a distinct record

#### Scenario: Resolve a concurrent duplicate registration
- **WHEN** concurrent requests attempt to register the same source identity
- **THEN** exactly one record is created and each rejected request receives `409 Conflict`

### Requirement: Retrieve document metadata by catalog ID
The system SHALL expose `GET /api/v1/documents/{documentId}` and return the complete stored metadata representation when the UUID exists.

#### Scenario: Retrieve an existing document
- **WHEN** a consumer requests an existing catalog UUID
- **THEN** the system returns `200 OK` with the complete metadata representation

#### Scenario: Retrieve an unknown document
- **WHEN** a consumer requests a valid UUID that does not exist
- **THEN** the system returns `404 Not Found` using the Problem Details error contract

#### Scenario: Reject a malformed document ID
- **WHEN** a consumer requests a document using a value that is not a UUID
- **THEN** the system returns `400 Bad Request` using the Problem Details error contract

### Requirement: List customer document metadata
The system SHALL expose `GET /api/v1/documents` with required `customerId` and optional `documentType`, `page`, and `size` query parameters. Results SHALL include only the exact case-sensitive `customerId`, SHALL optionally include only the requested document type, and SHALL be ordered by `documentDate` descending and then `id` descending.

#### Scenario: List a customer's documents
- **WHEN** a consumer lists metadata using a customer ID with matching records
- **THEN** the system returns only that customer's records in the defined order

#### Scenario: Filter a customer's documents by type
- **WHEN** a consumer supplies a supported document type with the customer ID
- **THEN** the system returns only records matching both the exact customer ID and document type

#### Scenario: List a customer with no documents
- **WHEN** a consumer lists metadata for a customer with no matching records
- **THEN** the system returns `200 OK` with an empty `items` collection and zero totals

### Requirement: Paginate list results
The system SHALL use zero-based page numbers, default `page` to `0`, default `size` to `20`, and limit `size` to `100`. A successful list response SHALL contain `items`, `page`, `size`, `totalElements`, and `totalPages` without exposing framework-specific page serialization.

#### Scenario: Use default pagination
- **WHEN** a consumer omits `page` and `size`
- **THEN** the system returns the first page with a requested size of 20 and the corresponding totals

#### Scenario: Request a specific valid page
- **WHEN** a consumer supplies a non-negative page and a size from 1 through 100
- **THEN** the system returns that page and the corresponding totals

#### Scenario: Reject invalid pagination
- **WHEN** a consumer supplies a negative page, a size less than 1, or a size greater than 100
- **THEN** the system returns `400 Bad Request` using the Problem Details error contract

#### Scenario: Reject a missing customer ID
- **WHEN** a consumer lists documents without a nonblank `customerId`
- **THEN** the system returns `400 Bad Request` with a field error identifying `customerId`

### Requirement: Return consistent API errors
The system SHALL represent `400`, `404`, `409`, and unexpected `500` responses using Spring-compatible Problem Details JSON. Each response SHALL contain `type`, `title`, `status`, and `detail`; validation responses SHALL additionally contain a structured `fieldErrors` extension. Error responses SHALL NOT expose Java exception names, stack traces, SQL, or database implementation details.

#### Scenario: Return validation problem details
- **WHEN** a request fails field validation
- **THEN** the response contains Problem Details fields and structured field errors without internal implementation details

#### Scenario: Return conflict problem details
- **WHEN** registration conflicts with an existing source identity
- **THEN** the response contains a `409` Problem Details representation without database implementation details

#### Scenario: Return unexpected failure problem details
- **WHEN** an unexpected server failure prevents request completion
- **THEN** the response contains a `500` Problem Details representation without internal exception or database details

### Requirement: Keep catalog metadata immutable
The initial capability SHALL provide no operation for updating or deleting registered document metadata.

#### Scenario: Use the documented API surface
- **WHEN** an internal application integrates with the initial document catalog capability
- **THEN** the available document operations are registration, retrieval by catalog ID, and paginated customer listing only

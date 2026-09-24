# Proposal

## Why

Producers cannot safely retry document registration after an uncertain outcome because every repeated source identity currently returns `409 Conflict`, even when the request describes exactly the resource already created. Registration should recognize an exact replay while continuing to reject attempts to change immutable metadata.

## What Changes

- Treat the normalized `(sourceSystem, sourceDocumentId)` source identity as the registration idempotency key.
- Return `201 Created` when a request creates a new document metadata record.
- Return `200 OK`, the original representation, and its resource `Location` when a valid request exactly matches every normalized producer-supplied field of an existing record.
- Continue returning the existing generic `409 Conflict` Problem Details response when the source identity exists with different metadata.
- Preserve validation-before-resolution, immutable metadata, case-sensitive identity semantics, and database-authoritative concurrency protection.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `document-catalog`: Change registration and source-identity behavior so exact retries are idempotent while mismatched duplicates remain conflicts.

## Impact

- Changes the successful response semantics of `POST /api/v1/documents` for exact duplicate requests from `409 Conflict` to `200 OK`; the request and response representations remain unchanged.
- Affects registration orchestration, HTTP response selection, persistence conflict handling, and registration/concurrency tests.
- Retains the existing PostgreSQL uniqueness constraint and requires no new endpoint, idempotency header, dependency, or database schema change.

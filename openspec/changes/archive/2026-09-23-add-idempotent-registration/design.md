# Design

## Context

See `proposal.md` for motivation and `specs/document-catalog/spec.md` for the behavioral contract. Registration currently performs an existence check followed by `saveAndFlush`, translating a named PostgreSQL unique-constraint violation into `DuplicateDocumentException`. The check improves the ordinary duplicate path but cannot arbitrate concurrent requests; the existing `(source_system, source_document_id)` unique constraint is the actual concurrency authority.

Recovering an existing row after a failed JPA flush is unsuitable for idempotency resolution because the transaction or persistence context may already be marked for rollback. The service therefore needs a conflict-aware persistence operation that does not rely on an exception as its expected concurrent control path.

## Goals / Non-Goals

**Goals:**

- Resolve both sequential and concurrent retries through one deterministic registration flow.
- Preserve the original stored representation and distinguish creation from replay at the controller boundary.
- Compare every normalized producer-supplied field explicitly while excluding generated fields.
- Keep the PostgreSQL unique constraint authoritative without changing the schema.

**Non-Goals:**

- Add a client-supplied idempotency key, endpoint, or request field.
- Update, merge, or otherwise reconcile different metadata for an existing source identity.
- Add a stored request fingerprint or change the database constraint.
- Generalize idempotency infrastructure beyond document registration.

## Decisions

### Use a conflict-aware insert followed by lookup

Replace the check-then-JPA-save registration path with a persistence operation equivalent to:

```sql
INSERT INTO document_metadata (...)
VALUES (...)
ON CONFLICT ON CONSTRAINT uk_document_metadata_source DO NOTHING
RETURNING id
```

The persistence layer will report whether the candidate was inserted. If it was not inserted, the application service will read the existing row by the normalized source identity in the same service transaction and compare it with the request.

This keeps the unique constraint as the single concurrency arbiter. Under PostgreSQL's default `READ COMMITTED` isolation, a conflicting insert waits for the competing transaction to resolve; a subsequent statement can then observe the committed winner. If the competing transaction rolls back, the insert can proceed instead.

Implement the operation as a small PostgreSQL-specific persistence component using the project's existing database access stack and explicit column mapping. This is preferred over catching `DataIntegrityViolationException` and attempting a read in the failed JPA transaction. It is also preferred over `ON CONFLICT DO UPDATE ... RETURNING`, because even a no-op update weakens the immutable-write model and could have observable effects if triggers or auditing are added later.

The existing unique constraint and migration remain unchanged. An early existence query is not retained because it cannot establish correctness and creates a separate code path with a race between lookup and insert.

### Return an explicit registration outcome

The application service will return a small outcome containing the resolved `DocumentMetadata` and a `created` indicator. The controller will always construct `Location` from the resolved document ID, return `201 Created` when `created` is true, and return `200 OK` when it is false.

This keeps HTTP status selection out of the persistence layer and ensures a replay uses the existing row's `id` and `recordedAt`. Returning only `DocumentMetadata`, as the service does today, cannot distinguish creation from replay without another lookup.

### Compare normalized producer metadata explicitly

Registration will construct or otherwise obtain a fully normalized candidate using the existing normalization rules before persistence resolution. When an existing row wins, a dedicated comparison will cover `sourceSystem`, `sourceDocumentId`, `customerId`, `documentType`, `filename`, `contentType`, `sizeBytes`, `storageReference`, and `documentDate`. It will deliberately exclude `id` and `recordedAt`.

The comparison will not use record-wide equality because that includes generated fields and would make all replays appear different. Exact typed equality after existing trimming is used; no new case folding, MIME normalization, storage-reference interpretation, or date transformation is introduced.

If any compared field differs, the service will throw the existing `DuplicateDocumentException`. The existing exception handler and generic `urn:problem:duplicate-document` response remain unchanged, and the stored record is never updated.

### Keep validation at the HTTP boundary before service invocation

The controller retains `@Valid` request-body validation and unknown-property rejection before mapping and service invocation. Consequently, an invalid request receives its existing `400` response even when its supplied source identity already exists. Domain validation remains a defensive invariant for non-HTTP callers and tests.

## Risks / Trade-offs

- [The conflict-aware insert uses PostgreSQL-specific SQL] -> The service and its integration tests already intentionally target PostgreSQL; isolate the SQL in persistence and cover it with Testcontainers.
- [Clients that assumed every successful registration returns `201`] -> Preserve the response body and `Location` shape, document `200` as the exact-replay outcome, and keep all mismatched duplicates on the existing `409` contract.
- [A comparison could omit a producer field during future model evolution] -> Centralize the comparison and add parameterized tests that vary each current producer-supplied field independently.
- [Concurrency tests can pass without exercising the collision window] -> Coordinate requests with latches and verify response outcomes, shared identity and timestamps, and a single persisted row against PostgreSQL.
- [A conflict-aware insert reports no row but the winner cannot be read] -> Treat this as an unexpected consistency failure rather than converting it to a replay or conflict; do not guess or create a second record.

## Migration Plan

No database migration is required. Deploy the application change against the existing schema and unique constraint, then verify new, sequential replay, mismatched replay, and concurrent registration behavior through PostgreSQL-backed tests.

Rollback requires only restoring the prior application version. Existing records remain compatible; exact replays will revert to the former `409 Conflict` behavior after rollback.

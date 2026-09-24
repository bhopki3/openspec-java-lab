# Tasks

## 1. Persistence Arbitration

- [ ] 1.1 Add a persistence operation that performs `INSERT ... ON CONFLICT ON CONSTRAINT uk_document_metadata_source DO NOTHING RETURNING` and reports whether the candidate was inserted; verify PostgreSQL repository tests cover new insertion, exact source-identity conflict, and unchanged case-sensitive identity behavior.
- [ ] 1.2 Add lookup by normalized `(sourceSystem, sourceDocumentId)` for a non-inserted candidate and remove the registration path's early existence check and unique-violation parsing; verify persistence tests show the existing row is readable after a conflict without modifying it.

## 2. Idempotent Registration Logic

- [ ] 2.1 Introduce an application registration outcome containing the resolved document and whether it was created, then update the service to return either the inserted candidate or the existing record; verify service tests assert the creation and replay outcome flags and preserve the original `id` and `recordedAt`.
- [ ] 2.2 Implement explicit equality across all normalized producer-supplied fields while excluding generated fields; verify parameterized service tests vary each field independently, accept whitespace-equivalent input, and reject every meaningful metadata difference with `DuplicateDocumentException` while leaving the original record unchanged.
- [ ] 2.3 Treat a conflict-aware insert with no readable existing row as an unexpected failure rather than a replay or metadata conflict; verify a focused service test exercises that consistency-failure path.

## 3. HTTP Contract

- [ ] 3.1 Update registration response construction so newly created records return `201 Created` and exact replays return `200 OK`, both with the resolved representation and resource `Location`; verify HTTP tests assert an identical replay returns the original `id`, original `recordedAt`, matching `Location`, and only one persisted row.
- [ ] 3.2 Preserve mismatched replay handling through the existing generic `409` Problem Details contract; verify HTTP tests assert the existing problem type and fields and confirm the stored record is unchanged.
- [ ] 3.3 Verify request validation precedes idempotency resolution by testing invalid and unknown-property requests against an already registered source identity and asserting the applicable `400` response rather than `200` or `409`.

## 4. Concurrent Behavior

- [ ] 4.1 Replace the concurrent duplicate-rejection expectation with a PostgreSQL Testcontainers test for identical concurrent requests; verify exactly one creation outcome, all remaining replay outcomes, one stored row, and the same `id` and `recordedAt` in every result.
- [ ] 4.2 Add a PostgreSQL Testcontainers test for concurrent requests sharing an identity but carrying different metadata; verify exactly one request creates the record, the other receives a conflict, and the persisted metadata matches the winner without subsequent mutation.

## 5. Verification

- [ ] 5.1 Run the complete Maven test suite and `openspec validate add-idempotent-registration --strict`; verify both complete successfully without changing the existing database migration or adding dependencies.

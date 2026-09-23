package com.example.documentcatalog.document.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record DocumentMetadata(
        UUID id,
        String sourceSystem,
        String sourceDocumentId,
        String customerId,
        DocumentType documentType,
        String filename,
        String contentType,
        long sizeBytes,
        String storageReference,
        LocalDate documentDate,
        Instant recordedAt) {

    public DocumentMetadata {
        id = Objects.requireNonNull(id, "id is required");
        sourceSystem = normalized(sourceSystem, "sourceSystem", 100);
        sourceDocumentId = normalized(sourceDocumentId, "sourceDocumentId", 200);
        customerId = normalized(customerId, "customerId", 100);
        documentType = Objects.requireNonNull(documentType, "documentType is required");
        filename = normalized(filename, "filename", 255);
        contentType = normalized(contentType, "contentType", 100);
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be greater than zero");
        }
        storageReference = normalized(storageReference, "storageReference", 500);
        documentDate = Objects.requireNonNull(documentDate, "documentDate is required");
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt is required");
    }

    private static String normalized(String value, String field, int maximumLength) {
        Objects.requireNonNull(value, field + " is required");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maximumLength + " characters");
        }
        return normalized;
    }
}

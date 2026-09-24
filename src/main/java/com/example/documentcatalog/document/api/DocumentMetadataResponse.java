package com.example.documentcatalog.document.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentType;

public record DocumentMetadataResponse(
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
}

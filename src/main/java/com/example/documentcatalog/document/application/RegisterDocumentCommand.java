package com.example.documentcatalog.document.application;

import java.time.LocalDate;

import com.example.documentcatalog.document.domain.DocumentType;

public record RegisterDocumentCommand(
        String sourceSystem,
        String sourceDocumentId,
        String customerId,
        DocumentType documentType,
        String filename,
        String contentType,
        long sizeBytes,
        String storageReference,
        LocalDate documentDate) {
}

package com.example.documentcatalog.document.api;

import java.time.LocalDate;

import com.example.documentcatalog.document.domain.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterDocumentRequest(
        @NotBlank @Size(max = 100) String sourceSystem,
        @NotBlank @Size(max = 200) String sourceDocumentId,
        @NotBlank @Size(max = 100) String customerId,
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 100) String contentType,
        @NotNull @Positive Long sizeBytes,
        @NotBlank @Size(max = 500) String storageReference,
        @NotNull LocalDate documentDate) {
}

package com.example.documentcatalog.document.application;

import java.util.Objects;

import com.example.documentcatalog.document.domain.DocumentMetadata;

public record DocumentRegistrationResult(DocumentMetadata document, boolean created) {

    public DocumentRegistrationResult {
        document = Objects.requireNonNull(document, "document is required");
    }
}

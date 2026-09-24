package com.example.documentcatalog.document.application;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID id) {
        super("Document metadata was not found for id " + id);
    }
}

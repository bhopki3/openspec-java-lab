package com.example.documentcatalog.document.application;

public class DuplicateDocumentException extends RuntimeException {

    public DuplicateDocumentException(String sourceSystem, String sourceDocumentId) {
        super("Document source identity already exists: " + sourceSystem + "/" + sourceDocumentId);
    }
}

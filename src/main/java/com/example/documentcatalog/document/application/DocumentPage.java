package com.example.documentcatalog.document.application;

import java.util.List;

import com.example.documentcatalog.document.domain.DocumentMetadata;

public record DocumentPage(
        List<DocumentMetadata> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public DocumentPage {
        items = List.copyOf(items);
    }
}

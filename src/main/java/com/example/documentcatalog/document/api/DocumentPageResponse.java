package com.example.documentcatalog.document.api;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentMetadataResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public DocumentPageResponse {
        items = List.copyOf(items);
    }
}

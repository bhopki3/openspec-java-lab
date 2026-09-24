package com.example.documentcatalog.document.api;

import com.example.documentcatalog.document.application.DocumentPage;
import com.example.documentcatalog.document.application.RegisterDocumentCommand;
import com.example.documentcatalog.document.domain.DocumentMetadata;
import org.springframework.stereotype.Component;

@Component
public class DocumentApiMapper {

    public RegisterDocumentCommand toCommand(RegisterDocumentRequest request) {
        return new RegisterDocumentCommand(
                request.sourceSystem().trim(), request.sourceDocumentId().trim(), request.customerId().trim(),
                request.documentType(), request.filename().trim(), request.contentType().trim(), request.sizeBytes(),
                request.storageReference().trim(), request.documentDate());
    }

    public DocumentMetadataResponse toResponse(DocumentMetadata document) {
        return new DocumentMetadataResponse(
                document.id(), document.sourceSystem(), document.sourceDocumentId(), document.customerId(),
                document.documentType(), document.filename(), document.contentType(), document.sizeBytes(),
                document.storageReference(), document.documentDate(), document.recordedAt());
    }

    public DocumentPageResponse toResponse(DocumentPage page) {
        return new DocumentPageResponse(
                page.items().stream().map(this::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}

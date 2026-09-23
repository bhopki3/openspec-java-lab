package com.example.documentcatalog.document.persistence;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import org.springframework.stereotype.Component;

@Component
public class DocumentMetadataMapper {

    public DocumentMetadataEntity toEntity(DocumentMetadata document) {
        return new DocumentMetadataEntity(
                document.id(), document.sourceSystem(), document.sourceDocumentId(), document.customerId(),
                document.documentType(), document.filename(), document.contentType(), document.sizeBytes(),
                document.storageReference(), document.documentDate(), document.recordedAt());
    }

    public DocumentMetadata toDomain(DocumentMetadataEntity entity) {
        return new DocumentMetadata(
                entity.getId(), entity.getSourceSystem(), entity.getSourceDocumentId(), entity.getCustomerId(),
                entity.getDocumentType(), entity.getFilename(), entity.getContentType(), entity.getSizeBytes(),
                entity.getStorageReference(), entity.getDocumentDate(), entity.getRecordedAt());
    }
}

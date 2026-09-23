package com.example.documentcatalog.document.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_metadata")
public class DocumentMetadataEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "source_document_id", nullable = false, length = 200)
    private String sourceDocumentId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_reference", nullable = false, length = 500)
    private String storageReference;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected DocumentMetadataEntity() {
    }

    public DocumentMetadataEntity(
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
        this.id = id;
        this.sourceSystem = sourceSystem;
        this.sourceDocumentId = sourceDocumentId;
        this.customerId = customerId;
        this.documentType = documentType;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageReference = storageReference;
        this.documentDate = documentDate;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSourceDocumentId() { return sourceDocumentId; }
    public String getCustomerId() { return customerId; }
    public DocumentType getDocumentType() { return documentType; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageReference() { return storageReference; }
    public LocalDate getDocumentDate() { return documentDate; }
    public Instant getRecordedAt() { return recordedAt; }
}

package com.example.documentcatalog.document.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataEntity;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import com.example.documentcatalog.document.persistence.DocumentRegistrationPersistence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentCatalogService {

    private final DocumentMetadataRepository repository;
    private final DocumentRegistrationPersistence registrationPersistence;
    private final DocumentMetadataMapper mapper;
    private final Clock clock;

    public DocumentCatalogService(
            DocumentMetadataRepository repository,
            DocumentRegistrationPersistence registrationPersistence,
            DocumentMetadataMapper mapper,
            Clock clock) {
        this.repository = repository;
        this.registrationPersistence = registrationPersistence;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DocumentRegistrationResult register(RegisterDocumentCommand command) {
        DocumentMetadata candidate = new DocumentMetadata(
                UUID.randomUUID(),
                command.sourceSystem(),
                command.sourceDocumentId(),
                command.customerId(),
                command.documentType(),
                command.filename(),
                command.contentType(),
                command.sizeBytes(),
                command.storageReference(),
                command.documentDate(),
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));

        if (registrationPersistence.insertIfAbsent(mapper.toEntity(candidate))) {
            return new DocumentRegistrationResult(candidate, true);
        }

        DocumentMetadata existing = repository.findBySourceSystemAndSourceDocumentId(
                        candidate.sourceSystem(), candidate.sourceDocumentId())
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "Source identity conflict was reported without an existing document"));

        if (!hasSameProducerMetadata(existing, candidate)) {
            throw new DuplicateDocumentException(candidate.sourceSystem(), candidate.sourceDocumentId());
        }
        return new DocumentRegistrationResult(existing, false);
    }

    private static boolean hasSameProducerMetadata(DocumentMetadata existing, DocumentMetadata candidate) {
        return existing.sourceSystem().equals(candidate.sourceSystem())
                && existing.sourceDocumentId().equals(candidate.sourceDocumentId())
                && existing.customerId().equals(candidate.customerId())
                && existing.documentType() == candidate.documentType()
                && existing.filename().equals(candidate.filename())
                && existing.contentType().equals(candidate.contentType())
                && existing.sizeBytes() == candidate.sizeBytes()
                && existing.storageReference().equals(candidate.storageReference())
                && existing.documentDate().equals(candidate.documentDate());
    }

    @Transactional(readOnly = true)
    public DocumentMetadata get(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public DocumentPage list(String customerId, DocumentType documentType, Integer page, Integer size) {
        String normalizedCustomerId = customerId == null ? "" : customerId.trim();
        if (normalizedCustomerId.isEmpty()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }

        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        if (resolvedPage < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (resolvedSize < 1 || resolvedSize > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        PageRequest request = PageRequest.of(resolvedPage, resolvedSize, Sort.by(
                Sort.Order.desc("documentDate"), Sort.Order.desc("id")));
        Page<DocumentMetadataEntity> result = documentType == null
                ? repository.findByCustomerId(normalizedCustomerId, request)
                : repository.findByCustomerIdAndDocumentType(normalizedCustomerId, documentType, request);

        return new DocumentPage(
                result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}

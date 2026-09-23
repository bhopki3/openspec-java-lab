package com.example.documentcatalog.document.application;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataEntity;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentCatalogService {

    private final DocumentMetadataRepository repository;
    private final DocumentMetadataMapper mapper;
    private final Clock clock;

    public DocumentCatalogService(DocumentMetadataRepository repository, DocumentMetadataMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DocumentMetadata register(RegisterDocumentCommand command) {
        String sourceSystem = command.sourceSystem().trim();
        String sourceDocumentId = command.sourceDocumentId().trim();
        if (repository.existsBySourceSystemAndSourceDocumentId(sourceSystem, sourceDocumentId)) {
            throw new DuplicateDocumentException(sourceSystem, sourceDocumentId);
        }

        DocumentMetadata document = new DocumentMetadata(
                UUID.randomUUID(),
                sourceSystem,
                sourceDocumentId,
                command.customerId(),
                command.documentType(),
                command.filename(),
                command.contentType(),
                command.sizeBytes(),
                command.storageReference(),
                command.documentDate(),
                Instant.now(clock));

        try {
            DocumentMetadataEntity saved = repository.saveAndFlush(mapper.toEntity(document));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isSourceIdentityUniqueConstraintViolation(exception)) {
                throw new DuplicateDocumentException(sourceSystem, sourceDocumentId);
            }
            throw exception;
        }
    }

    private static boolean isSourceIdentityUniqueConstraintViolation(Throwable failure) {
        boolean uniqueConstraintViolation = false;
        boolean sourceIdentityConstraintNamed = false;

        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                uniqueConstraintViolation = true;
            }
            if (cause.getMessage() != null
                    && cause.getMessage().contains("uk_document_metadata_source")) {
                sourceIdentityConstraintNamed = true;
            }
            if (cause == cause.getCause()) {
                break;
            }
        }
        return uniqueConstraintViolation && sourceIdentityConstraintNamed;
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

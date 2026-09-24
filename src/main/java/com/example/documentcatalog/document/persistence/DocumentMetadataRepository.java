package com.example.documentcatalog.document.persistence;

import java.util.Optional;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadataEntity, UUID> {

    boolean existsBySourceSystemAndSourceDocumentId(String sourceSystem, String sourceDocumentId);

    Optional<DocumentMetadataEntity> findBySourceSystemAndSourceDocumentId(
            String sourceSystem, String sourceDocumentId);

    Page<DocumentMetadataEntity> findByCustomerId(String customerId, Pageable pageable);

    Page<DocumentMetadataEntity> findByCustomerIdAndDocumentType(
            String customerId, DocumentType documentType, Pageable pageable);
}

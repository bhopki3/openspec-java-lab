package com.example.documentcatalog.document.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class DocumentMetadataRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    DocumentMetadataRepository repository;

    @Autowired
    DocumentMetadataMapper mapper;

    @Autowired
    DocumentRegistrationPersistence registrationPersistence;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void roundTripsDomainMetadataAndPreservesCase() {
        DocumentMetadata original = metadata(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Source-System", "Document-A", "Customer-A", DocumentType.STATEMENT,
                LocalDate.of(2026, 8, 31));

        repository.saveAndFlush(mapper.toEntity(original));

        assertThat(mapper.toDomain(repository.findById(original.id()).orElseThrow())).isEqualTo(original);
        assertThat(repository.existsBySourceSystemAndSourceDocumentId("Source-System", "Document-A")).isTrue();
        assertThat(repository.existsBySourceSystemAndSourceDocumentId("source-system", "Document-A")).isFalse();
    }

    @Test
    void databaseRejectsAnExactDuplicateButAllowsCaseDifferences() {
        repository.saveAndFlush(mapper.toEntity(metadata(UUID.randomUUID(), "Source", "Doc", "C", DocumentType.LETTER, LocalDate.now())));

        assertThatThrownBy(() -> repository.saveAndFlush(mapper.toEntity(
                metadata(UUID.randomUUID(), "Source", "Doc", "C", DocumentType.LETTER, LocalDate.now()))))
                .isInstanceOf(DataIntegrityViolationException.class);

        repository.saveAndFlush(mapper.toEntity(metadata(
                UUID.randomUUID(), "source", "Doc", "C", DocumentType.LETTER, LocalDate.now())));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void conflictAwareInsertReportsCreationConflictAndCaseSensitiveIdentity() {
        DocumentMetadata original = metadata(uuid(1), "Source", "Doc", "Customer-A",
                DocumentType.STATEMENT, LocalDate.of(2026, 8, 31));
        DocumentMetadata duplicate = metadata(uuid(2), "Source", "Doc", "Customer-B",
                DocumentType.LETTER, LocalDate.of(2026, 9, 1));
        DocumentMetadata caseDifference = metadata(uuid(3), "source", "Doc", "Customer-C",
                DocumentType.NOTICE, LocalDate.of(2026, 10, 1));

        assertThat(registrationPersistence.insertIfAbsent(mapper.toEntity(original))).isTrue();
        assertThat(registrationPersistence.insertIfAbsent(mapper.toEntity(duplicate))).isFalse();
        assertThat(registrationPersistence.insertIfAbsent(mapper.toEntity(caseDifference))).isTrue();

        assertThat(repository.count()).isEqualTo(2);
        assertThat(mapper.toDomain(repository.findBySourceSystemAndSourceDocumentId("Source", "Doc")
                .orElseThrow())).isEqualTo(original);
        assertThat(repository.findBySourceSystemAndSourceDocumentId("SOURCE", "Doc")).isEmpty();
    }

    @Test
    void conflictAwareInsertDoesNotIgnoreAnUnrelatedUniqueConstraintViolation() {
        DocumentMetadata original = metadata(uuid(1), "Source", "Doc", "Customer-A",
                DocumentType.STATEMENT, LocalDate.of(2026, 8, 31));
        DocumentMetadata duplicateCatalogId = metadata(uuid(1), "Other-Source", "Other-Doc", "Customer-B",
                DocumentType.LETTER, LocalDate.of(2026, 9, 1));

        assertThat(registrationPersistence.insertIfAbsent(mapper.toEntity(original))).isTrue();
        assertThatThrownBy(() -> registrationPersistence.insertIfAbsent(mapper.toEntity(duplicateCatalogId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(mapper.toDomain(repository.findById(original.id()).orElseThrow())).isEqualTo(original);
    }

    @Test
    void filtersAndOrdersCustomerPagesDeterministically() {
        repository.save(mapper.toEntity(metadata(uuid(1), "S", "D1", "Customer-A", DocumentType.LETTER, LocalDate.of(2026, 1, 1))));
        repository.save(mapper.toEntity(metadata(uuid(2), "S", "D2", "Customer-A", DocumentType.STATEMENT, LocalDate.of(2026, 2, 1))));
        repository.save(mapper.toEntity(metadata(uuid(3), "S", "D3", "Customer-A", DocumentType.STATEMENT, LocalDate.of(2026, 2, 1))));
        repository.save(mapper.toEntity(metadata(uuid(4), "S", "D4", "Customer-B", DocumentType.STATEMENT, LocalDate.of(2026, 3, 1))));
        repository.flush();

        PageRequest page = PageRequest.of(0, 2, Sort.by(
                Sort.Order.desc("documentDate"), Sort.Order.desc("id")));

        Page<DocumentMetadataEntity> all = repository.findByCustomerId("Customer-A", page);
        assertThat(all.getContent()).extracting(DocumentMetadataEntity::getId).containsExactly(uuid(3), uuid(2));
        assertThat(all.getTotalElements()).isEqualTo(3);

        Page<DocumentMetadataEntity> statements = repository.findByCustomerIdAndDocumentType(
                "Customer-A", DocumentType.STATEMENT, page);
        assertThat(statements.getContent()).extracting(DocumentMetadataEntity::getId).containsExactly(uuid(3), uuid(2));
        assertThat(repository.findByCustomerId("customer-a", page)).isEmpty();
    }

    private static DocumentMetadata metadata(
            UUID id, String sourceSystem, String sourceDocumentId, String customerId,
            DocumentType type, LocalDate date) {
        return new DocumentMetadata(
                id, sourceSystem, sourceDocumentId, customerId, type, "document.pdf", "application/pdf",
                123, "bucket/key", date, Instant.parse("2026-09-22T12:00:00Z"));
    }

    private static UUID uuid(long value) {
        return new UUID(0, value);
    }
}

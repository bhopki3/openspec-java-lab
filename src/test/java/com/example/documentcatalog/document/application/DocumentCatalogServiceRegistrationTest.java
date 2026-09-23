package com.example.documentcatalog.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DocumentCatalogServiceRegistrationTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");

    @Test
    void registersWithGeneratedIdentityAndClockTimeWhilePreservingStorageReference() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        DocumentMetadataMapper mapper = new DocumentMetadataMapper();
        when(repository.existsBySourceSystemAndSourceDocumentId("Source", "Document-1")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentCatalogService service = new DocumentCatalogService(
                repository, mapper, Clock.fixed(NOW, ZoneOffset.UTC));

        DocumentMetadata result = service.register(new RegisterDocumentCommand(
                " Source ", " Document-1 ", " Customer-A ", DocumentType.STATEMENT,
                " statement.pdf ", " application/pdf ", 123, " storage/Key ",
                LocalDate.of(2026, 12, 31)));

        assertThat(result.id()).isNotNull();
        assertThat(result.recordedAt()).isEqualTo(NOW);
        assertThat(result.sourceSystem()).isEqualTo("Source");
        assertThat(result.customerId()).isEqualTo("Customer-A");
        assertThat(result.storageReference()).isEqualTo("storage/Key");
        assertThat(result.documentDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void doesNotMisclassifyAnUnrelatedIntegrityFailureAsADuplicate() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        DocumentMetadataMapper mapper = new DocumentMetadataMapper();
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "positive size check failed",
                new SQLException("violates ck_document_metadata_size_positive", "23514"));
        when(repository.saveAndFlush(any())).thenThrow(failure);
        DocumentCatalogService service = new DocumentCatalogService(
                repository, mapper, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.register(command()))
                .isSameAs(failure);
    }

    private static RegisterDocumentCommand command() {
        return new RegisterDocumentCommand(
                "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31));
    }
}

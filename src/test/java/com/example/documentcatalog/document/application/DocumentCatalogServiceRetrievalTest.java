package com.example.documentcatalog.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import com.example.documentcatalog.document.persistence.DocumentRegistrationPersistence;
import org.junit.jupiter.api.Test;

class DocumentCatalogServiceRetrievalTest {

    @Test
    void retrievesExistingMetadata() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        DocumentMetadataMapper mapper = new DocumentMetadataMapper();
        DocumentMetadata document = document();
        when(repository.findById(document.id())).thenReturn(Optional.of(mapper.toEntity(document)));
        DocumentCatalogService service = new DocumentCatalogService(
                repository, mock(DocumentRegistrationPersistence.class), mapper, Clock.systemUTC());

        assertThat(service.get(document.id())).isEqualTo(document);
    }

    @Test
    void reportsUnknownMetadata() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        DocumentCatalogService service = new DocumentCatalogService(
                repository, mock(DocumentRegistrationPersistence.class),
                new DocumentMetadataMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private static DocumentMetadata document() {
        return new DocumentMetadata(
                UUID.randomUUID(), "Source", "Document-1", "Customer-A", DocumentType.NOTICE,
                "notice.pdf", "application/pdf", 99, "storage/key", LocalDate.of(2026, 9, 1),
                Instant.parse("2026-09-22T12:00:00Z"));
    }
}

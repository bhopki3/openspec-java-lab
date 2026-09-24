package com.example.documentcatalog.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import com.example.documentcatalog.document.persistence.DocumentRegistrationPersistence;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class DocumentCatalogServiceListingTest {

    @Test
    void appliesDefaultPageAndFixedSorting() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        DocumentMetadataMapper mapper = new DocumentMetadataMapper();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findByCustomerId(eq("Customer-A"), pageable.capture()))
                .thenReturn(new PageImpl<>(List.of(mapper.toEntity(document()))));
        DocumentCatalogService service = new DocumentCatalogService(
                repository, mock(DocumentRegistrationPersistence.class), mapper, Clock.systemUTC());

        DocumentPage result = service.list(" Customer-A ", null, null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("documentDate: DESC,id: DESC");
    }

    @Test
    void filtersByTypeAndReturnsRequestedEmptyPageMetadata() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        DocumentMetadataMapper mapper = new DocumentMetadataMapper();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findByCustomerIdAndDocumentType(eq("Customer-A"), eq(DocumentType.NOTICE), pageable.capture()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));
        DocumentCatalogService service = new DocumentCatalogService(
                repository, mock(DocumentRegistrationPersistence.class), mapper, Clock.systemUTC());

        DocumentPage result = service.list("Customer-A", DocumentType.NOTICE, 2, 5);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isZero();
        verify(repository).findByCustomerIdAndDocumentType(
                eq("Customer-A"), eq(DocumentType.NOTICE), eq(pageable.getValue()));
    }

    @Test
    void rejectsInvalidCustomerAndPagination() {
        DocumentCatalogService service = new DocumentCatalogService(
                mock(DocumentMetadataRepository.class), mock(DocumentRegistrationPersistence.class),
                new DocumentMetadataMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.list(" ", null, 0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list("Customer-A", null, -1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list("Customer-A", null, 0, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    private static DocumentMetadata document() {
        return new DocumentMetadata(
                UUID.randomUUID(), "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31),
                Instant.parse("2026-09-22T12:00:00Z"));
    }
}

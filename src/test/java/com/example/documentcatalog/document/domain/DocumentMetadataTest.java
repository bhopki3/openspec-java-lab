package com.example.documentcatalog.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DocumentMetadataTest {

    @Test
    void trimsStringsWhilePreservingCaseAndAcceptingFutureDates() {
        LocalDate futureDate = LocalDate.now().plusYears(1);

        DocumentMetadata metadata = metadata(DocumentType.STATEMENT, 1, futureDate);

        assertThat(metadata.sourceSystem()).isEqualTo("Statement-Service");
        assertThat(metadata.sourceDocumentId()).isEqualTo("Doc-123");
        assertThat(metadata.customerId()).isEqualTo("Customer-A");
        assertThat(metadata.filename()).isEqualTo("Statement.PDF");
        assertThat(metadata.storageReference()).isEqualTo("bucket/Key");
        assertThat(metadata.documentDate()).isEqualTo(futureDate);
    }

    @ParameterizedTest
    @EnumSource(DocumentType.class)
    void supportsEveryDefinedDocumentType(DocumentType type) {
        assertThat(metadata(type, 1, LocalDate.now()).documentType()).isEqualTo(type);
    }

    @Test
    void rejectsNonPositiveSize() {
        assertThatThrownBy(() -> metadata(DocumentType.NOTICE, 0, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sizeBytes");
    }

    @Test
    void rejectsBlankAndOversizedValues() {
        assertThatThrownBy(() -> new DocumentMetadata(
                UUID.randomUUID(), " ", "Doc-123", "Customer-A", DocumentType.LETTER,
                "letter.pdf", "application/pdf", 1, "key", LocalDate.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceSystem");

        assertThatThrownBy(() -> new DocumentMetadata(
                UUID.randomUUID(), "source", "x".repeat(201), "Customer-A", DocumentType.LETTER,
                "letter.pdf", "application/pdf", 1, "key", LocalDate.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceDocumentId");
    }

    private static DocumentMetadata metadata(DocumentType type, long sizeBytes, LocalDate documentDate) {
        return new DocumentMetadata(
                UUID.randomUUID(),
                " Statement-Service ",
                " Doc-123 ",
                " Customer-A ",
                type,
                " Statement.PDF ",
                " application/pdf ",
                sizeBytes,
                " bucket/Key ",
                documentDate,
                Instant.parse("2026-09-22T12:00:00Z"));
    }
}

package com.example.documentcatalog.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import com.example.documentcatalog.document.persistence.DocumentRegistrationPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;

class DocumentCatalogServiceRegistrationTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00.123456789Z");
    private static final Instant ORIGINAL_RECORDED_AT = Instant.parse("2026-09-20T09:30:00Z");
    private static final UUID ORIGINAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void reportsCreationWithGeneratedIdentityAndClockTime() {
        Fixture fixture = fixture();
        when(fixture.registrationPersistence.insertIfAbsent(any())).thenReturn(true);

        DocumentRegistrationResult result = fixture.service.register(paddedCommand());

        assertThat(result.created()).isTrue();
        assertThat(result.document().id()).isNotNull();
        assertThat(result.document().recordedAt()).isEqualTo(NOW.truncatedTo(ChronoUnit.MICROS));
        assertThat(result.document().sourceSystem()).isEqualTo("Source");
        assertThat(result.document().customerId()).isEqualTo("Customer-A");
        assertThat(result.document().storageReference()).isEqualTo("storage/key");
    }

    @Test
    void returnsOriginalResourceForWhitespaceEquivalentReplayAndIgnoresGeneratedFields() {
        Fixture fixture = fixtureWithExisting(original());

        DocumentRegistrationResult result = fixture.service.register(paddedCommand());

        assertThat(result.created()).isFalse();
        assertThat(result.document()).isEqualTo(original());
        assertThat(result.document().id()).isEqualTo(ORIGINAL_ID);
        assertThat(result.document().recordedAt()).isEqualTo(ORIGINAL_RECORDED_AT);
        assertThat(result.document().recordedAt()).isNotEqualTo(NOW);
    }

    @ParameterizedTest(name = "rejects changed {0}")
    @MethodSource("differentMetadataCommands")
    void rejectsEveryDifferentNonIdentityMetadataField(String field, RegisterDocumentCommand command) {
        Fixture fixture = fixtureWithExisting(original());

        assertThatThrownBy(() -> fixture.service.register(command))
                .isInstanceOf(DuplicateDocumentException.class);
        assertThat(fixture.mapper.toDomain(fixture.repository
                .findBySourceSystemAndSourceDocumentId("Source", "Document-1").orElseThrow()))
                .isEqualTo(original());
    }

    @ParameterizedTest(name = "creates distinct {0}")
    @MethodSource("differentIdentityCommands")
    void treatsDifferentCaseSensitiveIdentityAsCreation(String field, RegisterDocumentCommand command) {
        Fixture fixture = fixture();
        when(fixture.registrationPersistence.insertIfAbsent(any())).thenReturn(true);

        DocumentRegistrationResult result = fixture.service.register(command);

        assertThat(result.created()).isTrue();
    }

    @Test
    void reportsUnexpectedConsistencyFailureWhenConflictWinnerCannotBeRead() {
        Fixture fixture = fixture();
        when(fixture.registrationPersistence.insertIfAbsent(any())).thenReturn(false);
        when(fixture.repository.findBySourceSystemAndSourceDocumentId("Source", "Document-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixture.service.register(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without an existing document");
    }

    @Test
    void propagatesUnrelatedDatabaseFailuresWithoutTranslatingThemToDuplicateConflicts() {
        Fixture fixture = fixture();
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "unrelated database constraint failure");
        when(fixture.registrationPersistence.insertIfAbsent(any())).thenThrow(failure);

        assertThatThrownBy(() -> fixture.service.register(command()))
                .isSameAs(failure)
                .isNotInstanceOf(DuplicateDocumentException.class);
    }

    private static Stream<Arguments> differentMetadataCommands() {
        return Stream.of(
                Arguments.of("customerId", command("Customer-B", DocumentType.STATEMENT,
                        "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31))),
                Arguments.of("documentType", command("Customer-A", DocumentType.LETTER,
                        "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31))),
                Arguments.of("filename", command("Customer-A", DocumentType.STATEMENT,
                        "changed.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31))),
                Arguments.of("contentType", command("Customer-A", DocumentType.STATEMENT,
                        "statement.pdf", "text/plain", 123, "storage/key", LocalDate.of(2026, 8, 31))),
                Arguments.of("sizeBytes", command("Customer-A", DocumentType.STATEMENT,
                        "statement.pdf", "application/pdf", 456, "storage/key", LocalDate.of(2026, 8, 31))),
                Arguments.of("storageReference", command("Customer-A", DocumentType.STATEMENT,
                        "statement.pdf", "application/pdf", 123, "storage/other", LocalDate.of(2026, 8, 31))),
                Arguments.of("documentDate", command("Customer-A", DocumentType.STATEMENT,
                        "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 9, 1))));
    }

    private static Stream<Arguments> differentIdentityCommands() {
        return Stream.of(
                Arguments.of("sourceSystem", new RegisterDocumentCommand(
                        "source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                        "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31))),
                Arguments.of("sourceDocumentId", new RegisterDocumentCommand(
                        "Source", "document-1", "Customer-A", DocumentType.STATEMENT,
                        "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31))));
    }

    private static RegisterDocumentCommand paddedCommand() {
        return new RegisterDocumentCommand(
                " Source ", " Document-1 ", " Customer-A ", DocumentType.STATEMENT,
                " statement.pdf ", " application/pdf ", 123, " storage/key ", LocalDate.of(2026, 8, 31));
    }

    private static RegisterDocumentCommand command() {
        return command("Customer-A", DocumentType.STATEMENT, "statement.pdf", "application/pdf",
                123, "storage/key", LocalDate.of(2026, 8, 31));
    }

    private static RegisterDocumentCommand command(
            String customerId, DocumentType documentType, String filename, String contentType,
            long sizeBytes, String storageReference, LocalDate documentDate) {
        return new RegisterDocumentCommand(
                "Source", "Document-1", customerId, documentType, filename, contentType,
                sizeBytes, storageReference, documentDate);
    }

    private static DocumentMetadata original() {
        return new DocumentMetadata(
                ORIGINAL_ID, "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31),
                ORIGINAL_RECORDED_AT);
    }

    private static Fixture fixtureWithExisting(DocumentMetadata existing) {
        Fixture fixture = fixture();
        when(fixture.registrationPersistence.insertIfAbsent(any())).thenReturn(false);
        when(fixture.repository.findBySourceSystemAndSourceDocumentId("Source", "Document-1"))
                .thenReturn(Optional.of(fixture.mapper.toEntity(existing)));
        return fixture;
    }

    private static Fixture fixture() {
        DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
        DocumentRegistrationPersistence registrationPersistence = mock(DocumentRegistrationPersistence.class);
        DocumentMetadataMapper mapper = new DocumentMetadataMapper();
        DocumentCatalogService service = new DocumentCatalogService(
                repository, registrationPersistence, mapper, Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(repository, registrationPersistence, mapper, service);
    }

    private record Fixture(
            DocumentMetadataRepository repository,
            DocumentRegistrationPersistence registrationPersistence,
            DocumentMetadataMapper mapper,
            DocumentCatalogService service) {
    }
}

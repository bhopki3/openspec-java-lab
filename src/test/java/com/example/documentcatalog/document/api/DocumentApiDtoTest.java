package com.example.documentcatalog.document.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class DocumentApiDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final JsonMapper json = JsonMapper.builder()
            .findAndAddModules()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private final DocumentApiMapper mapper = new DocumentApiMapper();

    @Test
    void validatesEveryRegistrationFieldAndLengthBoundary() {
        RegisterDocumentRequest invalid = new RegisterDocumentRequest(
                "x".repeat(101), "x".repeat(201), "x".repeat(101), null,
                "x".repeat(256), "x".repeat(101), 0L, "x".repeat(501), null);

        Set<String> fields = validator.validate(invalid).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "sourceSystem", "sourceDocumentId", "customerId", "documentType", "filename",
                "contentType", "sizeBytes", "storageReference", "documentDate");
        RegisterDocumentRequest exactBoundary = new RegisterDocumentRequest(
                "x".repeat(100), "x".repeat(200), "x".repeat(100), DocumentType.CUSTOMER_UPLOAD,
                "x".repeat(255), "x".repeat(100), 1L, "x".repeat(500), LocalDate.of(2026, 8, 31));

        assertThat(validator.validate(exactBoundary)).isEmpty();
    }

    @Test
    void trimsTransportStringsWhenMappingToTheApplication() {
        RegisterDocumentRequest request = new RegisterDocumentRequest(
                " Source ", " Document-1 ", " Customer-A ", DocumentType.LETTER,
                " Letter.PDF ", " application/pdf ", 1L, " storage/Key ", LocalDate.of(2026, 9, 1));

        var command = mapper.toCommand(request);

        assertThat(command.sourceSystem()).isEqualTo("Source");
        assertThat(command.sourceDocumentId()).isEqualTo("Document-1");
        assertThat(command.customerId()).isEqualTo("Customer-A");
        assertThat(command.filename()).isEqualTo("Letter.PDF");
        assertThat(command.contentType()).isEqualTo("application/pdf");
        assertThat(command.storageReference()).isEqualTo("storage/Key");
    }

    @Test
    void rejectsBothZeroAndNegativeDocumentSizes() {
        RegisterDocumentRequest zero = requestWithSize(0);
        RegisterDocumentRequest negative = requestWithSize(-1);

        assertThat(validator.validate(zero)).extracting(violation -> violation.getPropertyPath().toString())
                .contains("sizeBytes");
        assertThat(validator.validate(negative)).extracting(violation -> violation.getPropertyPath().toString())
                .contains("sizeBytes");
    }

    @Test
    void serializesExplicitResponseAndRejectsUnsupportedTypesAndUnknownProperties() throws Exception {
        DocumentMetadataResponse response = mapper.toResponse(document());

        String encoded = json.writeValueAsString(response);

        assertThat(encoded).contains("\"documentType\":\"STATEMENT\"");
        assertThat(encoded).contains("\"recordedAt\":\"2026-09-22T12:00:00Z\"");
        assertThatThrownBy(() -> json.readValue(validJson().replace("STATEMENT", "OTHER"), RegisterDocumentRequest.class))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> json.readValue(validJson().replace("}", ",\"unexpected\":true}"), RegisterDocumentRequest.class))
                .isInstanceOf(Exception.class);
    }

    private static RegisterDocumentRequest validRequest() {
        return new RegisterDocumentRequest(
                "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 1L, "storage/key", LocalDate.of(2026, 8, 31));
    }

    private static RegisterDocumentRequest requestWithSize(long sizeBytes) {
        return new RegisterDocumentRequest(
                "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", sizeBytes, "storage/key", LocalDate.of(2026, 8, 31));
    }

    private static DocumentMetadata document() {
        return new DocumentMetadata(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 1, "storage/key", LocalDate.of(2026, 8, 31),
                Instant.parse("2026-09-22T12:00:00Z"));
    }

    private static String validJson() {
        return """
                {"sourceSystem":"Source","sourceDocumentId":"Document-1","customerId":"Customer-A",
                "documentType":"STATEMENT","filename":"statement.pdf","contentType":"application/pdf",
                "sizeBytes":1,"storageReference":"storage/key","documentDate":"2026-08-31"}
                """;
    }
}

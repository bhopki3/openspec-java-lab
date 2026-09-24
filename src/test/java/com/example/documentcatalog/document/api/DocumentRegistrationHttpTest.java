package com.example.documentcatalog.document.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentRegistrationHttpTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DocumentMetadataRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(DocumentType.class)
    void registersEverySupportedTypeWithFutureDateAndOpaqueStorageReference(DocumentType type) throws Exception {
        String sourceDocumentId = "Document-" + type;

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(sourceDocumentId, type)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        "http://localhost/api/v1/documents/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.sourceSystem").value("Source-System"))
                .andExpect(jsonPath("$.sourceDocumentId").value(sourceDocumentId))
                .andExpect(jsonPath("$.customerId").value("Customer-A"))
                .andExpect(jsonPath("$.documentType").value(type.name()))
                .andExpect(jsonPath("$.filename").value("Document.PDF"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(123))
                .andExpect(jsonPath("$.storageReference").value("storage/Opaque-Key"))
                .andExpect(jsonPath("$.documentDate").value("2030-12-31"))
                .andExpect(jsonPath("$.recordedAt").isNotEmpty());
    }

    @Test
    void returnsOriginalResourceAndLocationForAnIdenticalNormalizedReplay() throws Exception {
        String paddedRequest = requestJson("Document-STATEMENT", DocumentType.STATEMENT);
        MvcResult first = mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paddedRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String firstBody = first.getResponse().getContentAsString();
        String id = JsonPath.read(firstBody, "$.id");
        String recordedAt = JsonPath.read(firstBody, "$.recordedAt");
        String location = first.getResponse().getHeader("Location");

        MvcResult replay = mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paddedRequest
                                .replace(" Source-System ", "Source-System")
                                .replace(" Document-STATEMENT ", "Document-STATEMENT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Location", location))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.recordedAt").value(recordedAt))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(replay.getResponse().getContentAsString()).isEqualTo(firstBody);
        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void returnsExistingGenericConflictForDifferentMetadataAndPreservesStoredRecord() throws Exception {
        String request = requestJson("Document-STATEMENT", DocumentType.STATEMENT);
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("\"sizeBytes\": 123", "\"sizeBytes\": 456")))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:duplicate-document"))
                .andExpect(jsonPath("$.title").value("Duplicate document"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").isNotEmpty());
        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(repository
                .findBySourceSystemAndSourceDocumentId("Source-System", "Document-STATEMENT")
                .orElseThrow().getSizeBytes()).isEqualTo(123);
    }

    @Test
    void validatesBeforeResolvingAnExistingSourceIdentity() throws Exception {
        String request = requestJson("Document-STATEMENT", DocumentType.STATEMENT);
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("\"sizeBytes\": 123", "\"sizeBytes\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.fieldErrors.sizeBytes").isArray());

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("}", ", \"unknown\": true}")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        org.assertj.core.api.Assertions.assertThat(repository.count()).isEqualTo(1);
    }

    private static String requestJson(String sourceDocumentId, DocumentType type) {
        return """
                {
                  "sourceSystem": " Source-System ",
                  "sourceDocumentId": " %s ",
                  "customerId": " Customer-A ",
                  "documentType": "%s",
                  "filename": " Document.PDF ",
                  "contentType": " application/pdf ",
                  "sizeBytes": 123,
                  "storageReference": " storage/Opaque-Key ",
                  "documentDate": "2030-12-31"
                }
                """.formatted(sourceDocumentId, type.name());
    }
}

package com.example.documentcatalog.document.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.documentcatalog.document.domain.DocumentMetadata;
import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataMapper;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentRetrievalAndListingHttpTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DocumentMetadataRepository repository;

    @Autowired
    DocumentMetadataMapper mapper;

    @BeforeEach
    void seedDatabase() {
        repository.deleteAll();
        save(uuid(1), "D1", "Customer-A", DocumentType.LETTER, LocalDate.of(2026, 1, 1));
        save(uuid(2), "D2", "Customer-A", DocumentType.STATEMENT, LocalDate.of(2026, 2, 1));
        save(uuid(3), "D3", "Customer-A", DocumentType.STATEMENT, LocalDate.of(2026, 2, 1));
        save(uuid(4), "D4", "Customer-B", DocumentType.STATEMENT, LocalDate.of(2026, 3, 1));
    }

    @Test
    void retrievesExistingAndReportsMalformedAndUnknownIds() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid(1).toString()))
                .andExpect(jsonPath("$.sourceDocumentId").value("D1"));

        assertProblem(mockMvc.perform(get("/api/v1/documents/not-a-uuid")), 400);

        assertProblem(mockMvc.perform(get("/api/v1/documents/{id}", uuid(99))), 404);
    }

    @Test
    void listsOnlyOneCustomerWithDefaultsAndDeterministicOrdering() throws Exception {
        mockMvc.perform(get("/api/v1/documents").param("customerId", "Customer-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].id").value(uuid(3).toString()))
                .andExpect(jsonPath("$.items[1].id").value(uuid(2).toString()))
                .andExpect(jsonPath("$.items[2].id").value(uuid(1).toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/v1/documents").param("customerId", "customer-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtersAndPaginatesAndReturnsEmptyResults() throws Exception {
        mockMvc.perform(get("/api/v1/documents")
                        .param("customerId", "Customer-A")
                        .param("documentType", "STATEMENT")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(uuid(2).toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/documents").param("customerId", "Customer-C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void rejectsMissingCustomerUnsupportedTypeAndInvalidPagination() throws Exception {
        assertProblem(mockMvc.perform(get("/api/v1/documents")), 400)
                .andExpect(jsonPath("$.fieldErrors.customerId").isArray());
        assertProblem(mockMvc.perform(get("/api/v1/documents").param("customerId", "   ")), 400)
                .andExpect(jsonPath("$.fieldErrors.customerId").isArray());
        assertProblem(mockMvc.perform(get("/api/v1/documents")
                .param("customerId", "Customer-A").param("documentType", "OTHER")), 400);
        assertProblem(mockMvc.perform(get("/api/v1/documents")
                .param("customerId", "Customer-A").param("page", "-1")), 400);
        assertProblem(mockMvc.perform(get("/api/v1/documents")
                .param("customerId", "Customer-A").param("size", "0")), 400);
        assertProblem(mockMvc.perform(get("/api/v1/documents")
                .param("customerId", "Customer-A").param("size", "101")), 400);
    }

    private static org.springframework.test.web.servlet.ResultActions assertProblem(
            org.springframework.test.web.servlet.ResultActions action, int status) throws Exception {
        return action
                .andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").isNotEmpty())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private void save(UUID id, String sourceDocumentId, String customerId, DocumentType type, LocalDate date) {
        repository.save(mapper.toEntity(new DocumentMetadata(
                id, "Source", sourceDocumentId, customerId, type, "document.pdf", "application/pdf",
                123, "storage/key", date, Instant.parse("2026-09-22T12:00:00Z"))));
    }

    private static UUID uuid(long value) {
        return new UUID(0, value);
    }
}

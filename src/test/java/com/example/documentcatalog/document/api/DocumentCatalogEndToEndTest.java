package com.example.documentcatalog.document.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentCatalogEndToEndTest {

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
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void registersRetrievesAndListsPersistedMetadata() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceSystem":"statement-service","sourceDocumentId":"stmt-2026-08-C12345",
                                "customerId":"C12345","documentType":"STATEMENT","filename":"statement-2026-08.pdf",
                                "contentType":"application/pdf","sizeBytes":248392,
                                "storageReference":"documents/2026/08/abc123","documentDate":"2026-08-31"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String idText = JsonPath.read(registration.getResponse().getContentAsString(), "$.id");
        UUID id = UUID.fromString(idText);
        assertThat(registration.getResponse().getHeader("Location"))
                .isEqualTo("http://localhost/api/v1/documents/" + id);

        var persisted = mapper.toDomain(repository.findById(id).orElseThrow());
        assertThat(persisted.sourceSystem()).isEqualTo("statement-service");
        assertThat(persisted.sourceDocumentId()).isEqualTo("stmt-2026-08-C12345");
        assertThat(persisted.storageReference()).isEqualTo("documents/2026/08/abc123");
        assertThat(persisted.recordedAt()).isNotNull();

        mockMvc.perform(get("/api/v1/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idText))
                .andExpect(jsonPath("$.filename").value("statement-2026-08.pdf"));

        mockMvc.perform(get("/api/v1/documents").param("customerId", "C12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(idText))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}

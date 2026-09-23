package com.example.documentcatalog.common.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.example.documentcatalog.document.api.DocumentApiMapper;
import com.example.documentcatalog.document.api.DocumentController;
import com.example.documentcatalog.document.application.DocumentCatalogService;
import com.example.documentcatalog.document.application.DocumentNotFoundException;
import com.example.documentcatalog.document.application.DuplicateDocumentException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(DocumentController.class)
@Import({DocumentApiMapper.class, ApiExceptionHandler.class})
class ApiErrorContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DocumentCatalogService service;

    @Test
    void returnsStructuredFieldErrorsForInvalidBody() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceSystem":" ","sourceDocumentId":"D","customerId":"C",
                                "documentType":"STATEMENT","filename":"f.pdf","contentType":"application/pdf",
                                "sizeBytes":0,"storageReference":"key","documentDate":"2026-08-31"}
                                """)), 400)
                .andExpect(jsonPath("$.fieldErrors.sourceSystem").isArray())
                .andExpect(jsonPath("$.fieldErrors.sizeBytes").isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sourceSystem", "sourceDocumentId", "customerId", "documentType", "filename",
            "contentType", "sizeBytes", "storageReference", "documentDate"
    })
    void identifiesEveryOmittedRequiredRegistrationField(String field) throws Exception {
        Map<String, Object> request = validRequest();
        request.remove(field);

        assertProblem(mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(request))), 400)
                .andExpect(jsonPath("$.fieldErrors." + field).isArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sourceSystem", "sourceDocumentId", "customerId", "filename", "contentType", "storageReference"
    })
    void identifiesEveryBlankRequiredStringField(String field) throws Exception {
        Map<String, Object> request = validRequest();
        request.put(field, "   ");

        assertProblem(mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(request))), 400)
                .andExpect(jsonPath("$.fieldErrors." + field).isArray());
    }

    @Test
    void returnsBadRequestForMalformedUnknownAndUnsupportedJson() throws Exception {
        assertProblem(mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON).content("{")), 400);
        assertProblem(mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON).content(validJson().replace("}", ",\"unknown\":true}"))), 400);
        assertProblem(mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON).content(validJson().replace("STATEMENT", "OTHER"))), 400);
    }

    @Test
    void returnsNotFoundConflictAndUnexpectedProblemDetailsWithoutInternals() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new DocumentNotFoundException(id));
        assertProblem(mockMvc.perform(get("/api/v1/documents/{id}", id)), 404);

        when(service.register(any())).thenThrow(new DuplicateDocumentException("Source", "D"));
        assertProblem(mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON).content(validJson())), 409);

        UUID failingId = UUID.randomUUID();
        when(service.get(failingId)).thenThrow(new RuntimeException("SQL database password secret"));
        assertProblem(mockMvc.perform(get("/api/v1/documents/{id}", failingId)), 500)
                .andExpect(content().string(Matchers.not(Matchers.containsString("SQL"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("database"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("RuntimeException"))));
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

    private static String validJson() {
        return """
                {"sourceSystem":"Source","sourceDocumentId":"D","customerId":"C",
                "documentType":"STATEMENT","filename":"f.pdf","contentType":"application/pdf",
                "sizeBytes":1,"storageReference":"key","documentDate":"2026-08-31"}
                """;
    }

    private static Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sourceSystem", "Source");
        request.put("sourceDocumentId", "D");
        request.put("customerId", "C");
        request.put("documentType", "STATEMENT");
        request.put("filename", "f.pdf");
        request.put("contentType", "application/pdf");
        request.put("sizeBytes", 1);
        request.put("storageReference", "key");
        request.put("documentDate", "2026-08-31");
        return request;
    }
}

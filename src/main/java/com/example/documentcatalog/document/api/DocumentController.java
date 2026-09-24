package com.example.documentcatalog.document.api;

import java.net.URI;
import java.util.UUID;

import com.example.documentcatalog.document.application.DocumentCatalogService;
import com.example.documentcatalog.document.domain.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/documents")
@Validated
public class DocumentController {

    private final DocumentCatalogService service;
    private final DocumentApiMapper mapper;

    public DocumentController(DocumentCatalogService service, DocumentApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    ResponseEntity<DocumentMetadataResponse> register(@Valid @RequestBody RegisterDocumentRequest request) {
        DocumentMetadataResponse response = mapper.toResponse(service.register(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{documentId}")
    DocumentMetadataResponse get(@PathVariable UUID documentId) {
        return mapper.toResponse(service.get(documentId));
    }

    @GetMapping
    DocumentPageResponse list(
            @RequestParam @NotBlank @Size(max = 100) String customerId,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        return mapper.toResponse(service.list(customerId, documentType, page, size));
    }
}

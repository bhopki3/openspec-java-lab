package com.example.documentcatalog.document.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.documentcatalog.document.domain.DocumentType;
import com.example.documentcatalog.document.persistence.DocumentMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class DuplicateRegistrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    DocumentCatalogService service;

    @Autowired
    DocumentMetadataRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void rejectsAnOrdinaryDuplicate() {
        service.register(command());

        assertThatThrownBy(() -> service.register(command()))
                .isInstanceOf(DuplicateDocumentException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void databaseConstraintAllowsExactlyOneConcurrentRegistration() throws Exception {
        int attempts = 4;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int attempt = 0; attempt < attempts; attempt++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        service.register(command());
                        return true;
                    } catch (DuplicateDocumentException expected) {
                        return false;
                    }
                }));
            }

            ready.await();
            start.countDown();

            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(repository.count()).isEqualTo(1);
        }
    }

    private static RegisterDocumentCommand command() {
        return new RegisterDocumentCommand(
                "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31));
    }
}

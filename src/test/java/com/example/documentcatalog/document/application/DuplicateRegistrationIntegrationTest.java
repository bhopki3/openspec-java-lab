package com.example.documentcatalog.document.application;

import static org.assertj.core.api.Assertions.assertThat;
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
    void returnsTheOriginalResourceForAnOrdinaryReplay() {
        DocumentRegistrationResult created = service.register(command());
        DocumentRegistrationResult replay = service.register(command());

        assertThat(created.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.document()).isEqualTo(created.document());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void databaseConstraintResolvesConcurrentIdenticalRegistrationsToOneResource() throws Exception {
        int attempts = 4;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<DocumentRegistrationResult>> results = new ArrayList<>();
            for (int attempt = 0; attempt < attempts; attempt++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.register(command());
                }));
            }

            ready.await();
            start.countDown();

            List<DocumentRegistrationResult> completed = results.stream()
                    .map(DuplicateRegistrationIntegrationTest::get)
                    .toList();
            assertThat(completed).filteredOn(DocumentRegistrationResult::created).hasSize(1);
            assertThat(completed).extracting(result -> result.document().id())
                    .containsOnly(completed.getFirst().document().id());
            assertThat(completed).extracting(result -> result.document().recordedAt())
                    .containsOnly(completed.getFirst().document().recordedAt());
            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Test
    void databaseConstraintResolvesConcurrentDifferentMetadataAsCreationAndConflict() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Attempt>> results = List.of(
                    executor.submit(() -> attempt(command(), ready, start)),
                    executor.submit(() -> attempt(differentMetadataCommand(), ready, start)));

            ready.await();
            start.countDown();

            List<Attempt> completed = results.stream()
                    .map(DuplicateRegistrationIntegrationTest::get)
                    .toList();
            assertThat(completed).filteredOn(attempt -> attempt.result() != null).hasSize(1);
            assertThat(completed).filteredOn(Attempt::conflict).hasSize(1);

            DocumentRegistrationResult winner = completed.stream()
                    .map(Attempt::result)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
            assertThat(winner.created()).isTrue();
            assertThat(repository.count()).isEqualTo(1);
            assertThat(repository.findBySourceSystemAndSourceDocumentId("Source", "Document-1")
                    .map(entity -> entity.getCustomerId()).orElseThrow())
                    .isEqualTo(winner.document().customerId());
        }
    }

    private Attempt attempt(
            RegisterDocumentCommand command, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return new Attempt(service.register(command), false);
        } catch (DuplicateDocumentException expected) {
            return new Attempt(null, true);
        }
    }

    private static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static RegisterDocumentCommand command() {
        return new RegisterDocumentCommand(
                "Source", "Document-1", "Customer-A", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31));
    }

    private static RegisterDocumentCommand differentMetadataCommand() {
        return new RegisterDocumentCommand(
                "Source", "Document-1", "Customer-B", DocumentType.STATEMENT,
                "statement.pdf", "application/pdf", 123, "storage/key", LocalDate.of(2026, 8, 31));
    }

    private record Attempt(DocumentRegistrationResult result, boolean conflict) {
    }
}

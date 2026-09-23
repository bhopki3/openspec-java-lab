package com.example.documentcatalog.document.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class DatabaseMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationCreatesTableAndCaseSensitiveSourceIdentityConstraint() {
        insert("Source", "Document-1", 10);

        assertThatThrownBy(() -> insert("Source", "Document-1", 10))
                .isInstanceOf(DataIntegrityViolationException.class);

        insert("source", "Document-1", 10);

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM document_metadata", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void migrationEnforcesPositiveSize() {
        assertThatThrownBy(() -> insert("Source", "Invalid-size", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insert(String sourceSystem, String sourceDocumentId, long sizeBytes) {
        jdbc.update("""
                INSERT INTO document_metadata (
                    id, source_system, source_document_id, customer_id, document_type,
                    filename, content_type, size_bytes, storage_reference, document_date, recorded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), sourceSystem, sourceDocumentId, "Customer-A", "STATEMENT",
                "statement.pdf", "application/pdf", sizeBytes, "bucket/key",
                Date.valueOf(LocalDate.of(2026, 8, 31)), Timestamp.from(Instant.parse("2026-09-22T12:00:00Z")));
    }
}

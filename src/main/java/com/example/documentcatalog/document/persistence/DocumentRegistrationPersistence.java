package com.example.documentcatalog.document.persistence;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentRegistrationPersistence {

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO document_metadata (
                id, source_system, source_document_id, customer_id, document_type,
                filename, content_type, size_bytes, storage_reference, document_date, recorded_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uk_document_metadata_source DO NOTHING
            RETURNING id
            """;

    private final JdbcTemplate jdbc;

    public DocumentRegistrationPersistence(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertIfAbsent(DocumentMetadataEntity document) {
        return jdbc.query(INSERT_IF_ABSENT, statement -> {
            statement.setObject(1, document.getId());
            statement.setString(2, document.getSourceSystem());
            statement.setString(3, document.getSourceDocumentId());
            statement.setString(4, document.getCustomerId());
            statement.setString(5, document.getDocumentType().name());
            statement.setString(6, document.getFilename());
            statement.setString(7, document.getContentType());
            statement.setLong(8, document.getSizeBytes());
            statement.setString(9, document.getStorageReference());
            statement.setObject(10, document.getDocumentDate());
            statement.setTimestamp(11, Timestamp.from(document.getRecordedAt()));
        }, (ResultSetExtractor<Boolean>) resultSet -> resultSet.next());
    }
}

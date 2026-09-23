CREATE TABLE document_metadata (
    id UUID PRIMARY KEY,
    source_system VARCHAR(100) NOT NULL,
    source_document_id VARCHAR(200) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_reference VARCHAR(500) NOT NULL,
    document_date DATE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_document_metadata_source UNIQUE (source_system, source_document_id),
    CONSTRAINT ck_document_metadata_size_positive CHECK (size_bytes > 0)
);

CREATE INDEX idx_document_metadata_customer_type_date
    ON document_metadata (customer_id, document_type, document_date DESC, id DESC);

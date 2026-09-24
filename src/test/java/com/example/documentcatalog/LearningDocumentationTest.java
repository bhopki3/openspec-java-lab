package com.example.documentcatalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LearningDocumentationTest {

    @Test
    void documentsExecutableSetupTestingRuntimeMigrationAndOpenSpecSteps() throws IOException {
        String readme = Files.readString(Path.of("README.md"));

        assertThat(readme)
                .contains("Java 21")
                .contains("./mvnw clean verify")
                .contains("docker run --name document-catalog-postgres")
                .contains("./mvnw spring-boot:run")
                .contains("DATABASE_URL")
                .contains("Flyway")
                .contains("Testcontainers")
                .contains("openspec-apply-change")
                .contains("openspec-archive-change");
        assertThat(Path.of("mvnw")).isExecutable();
        assertThat(Path.of("src/main/resources/db/migration/V1__create_document_metadata.sql")).exists();
    }
}

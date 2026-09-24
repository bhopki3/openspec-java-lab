package com.example.documentcatalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SpecificationTraceabilityTest {

    @Test
    void everyDocumentCatalogScenarioIsMappedInTheReadme() throws IOException {
        Path spec = Path.of("openspec/specs/document-catalog/spec.md");
        String readme = Files.readString(Path.of("README.md"));
        List<String> scenarios = Files.readAllLines(spec).stream()
                .filter(line -> line.startsWith("#### Scenario: "))
                .map(line -> line.substring("#### Scenario: ".length()))
                .toList();

        assertThat(scenarios).isNotEmpty();
        assertThat(scenarios).allSatisfy(scenario ->
                assertThat(readme).contains("Scenario: **" + scenario + "**"));
    }
}

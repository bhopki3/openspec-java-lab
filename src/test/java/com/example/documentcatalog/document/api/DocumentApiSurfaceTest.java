package com.example.documentcatalog.document.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class DocumentApiSurfaceTest {

    @Test
    void exposesOnlyRegistrationRetrievalAndListing() {
        Method[] methods = DocumentController.class.getDeclaredMethods();

        assertThat(methods)
                .filteredOn(method -> !method.isSynthetic())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("register", "get", "list");
        assertThat(Arrays.stream(methods).filter(method -> method.isAnnotationPresent(PostMapping.class))).hasSize(1);
        assertThat(Arrays.stream(methods).filter(method -> method.isAnnotationPresent(GetMapping.class))).hasSize(2);
        assertThat(Arrays.stream(methods).filter(method -> method.isAnnotationPresent(PutMapping.class))).isEmpty();
        assertThat(Arrays.stream(methods).filter(method -> method.isAnnotationPresent(PatchMapping.class))).isEmpty();
        assertThat(Arrays.stream(methods).filter(method -> method.isAnnotationPresent(DeleteMapping.class))).isEmpty();
    }
}

package com.example.documentcatalog.common.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.documentcatalog.document.application.DuplicateDocumentException;
import com.example.documentcatalog.document.application.DocumentNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBodyValidation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), ignored -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return validationProblem("One or more request fields are invalid.", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            fieldErrors.computeIfAbsent(field, ignored -> new java.util.ArrayList<>())
                    .add(violation.getMessage());
        });
        return validationProblem("One or more request parameters are invalid.", fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        return validationProblem("A required request parameter is missing.",
                Map.of(exception.getParameterName(), List.of("must be provided")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body",
                "The request body is malformed or contains an unsupported value.", "malformed-request");
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail handleNotFound(DocumentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Document not found", exception.getMessage(), "document-not-found");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter",
                "The request contains an invalid parameter value.", "invalid-parameter");
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    ProblemDetail handleDuplicate(DuplicateDocumentException exception) {
        return problem(HttpStatus.CONFLICT, "Duplicate document",
                "A document with the same source identity is already registered.", "duplicate-document");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "The request contains an invalid value.", "invalid-request");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected request failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "The request could not be completed.", "internal-error");
    }

    private static ProblemDetail validationProblem(String detail, Map<String, List<String>> fieldErrors) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, "Validation failed", detail, "validation-failed");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:problem:" + type));
        return problem;
    }
}

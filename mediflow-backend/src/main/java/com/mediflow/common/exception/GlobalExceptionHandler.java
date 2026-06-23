package com.mediflow.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(
        ResponseStatusException exception,
        HttpServletRequest request
    ) {
        String message = exception.getReason() != null
            ? exception.getReason()
            : "Request could not be completed";

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            exception.getStatusCode(),
            message
        );

        problem.setTitle(
            HttpStatus.valueOf(exception.getStatusCode().value())
                .getReasonPhrase()
        );

        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", request.getRequestURI());

        return ResponseEntity
            .status(exception.getStatusCode())
            .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                validationErrors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
        );

        problem.setTitle("Validation failed");
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("errors", validationErrors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problem);
    }
}
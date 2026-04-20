package com.assignment.guardrailgateway.exception;

import org.springframework.http.HttpStatus;

public class GuardrailViolationException extends RuntimeException {

    private final HttpStatus status;

    public GuardrailViolationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

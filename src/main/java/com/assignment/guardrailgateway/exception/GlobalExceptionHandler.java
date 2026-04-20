package com.assignment.guardrailgateway.exception;

import com.assignment.guardrailgateway.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(ex.getMessage()));
    }

    @ExceptionHandler(GuardrailViolationException.class)
    public ResponseEntity<ApiResponse> handleGuardrail(GuardrailViolationException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ApiResponse("Validation failed"));
    }
}

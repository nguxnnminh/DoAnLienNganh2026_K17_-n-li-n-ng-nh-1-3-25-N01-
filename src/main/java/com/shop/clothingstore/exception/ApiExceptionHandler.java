package com.shop.clothingstore.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shop.clothingstore.dto.api.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.shop.clothingstore.controller.api")
@Order(1)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // =====================================================
    // 400 - Validation errors
    // =====================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {} | URI: {}", message, request.getRequestURI());

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(400, "VALIDATION_ERROR", message, request.getRequestURI())
        );
    }

    // =====================================================
    // 400 - Illegal state (cart empty, invalid operation)
    // =====================================================
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {

        log.warn("Illegal state: {} | URI: {}", ex.getMessage(), request.getRequestURI());

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(400, "INVALID_STATE", ex.getMessage(), request.getRequestURI())
        );
    }

    // =====================================================
    // 400 - Bad input
    // =====================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Invalid input: {} | URI: {}", ex.getMessage(), request.getRequestURI());

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(400, "INVALID_INPUT", ex.getMessage(), request.getRequestURI())
        );
    }

    // =====================================================
    // 401 - Bad credentials
    // =====================================================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiErrorResponse(401, "UNAUTHORIZED", "Email hoặc mật khẩu không đúng", request.getRequestURI())
        );
    }

    // =====================================================
    // 404 - Resource not found
    // =====================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiErrorResponse(404, "NOT_FOUND", ex.getMessage(), request.getRequestURI())
        );
    }

    // =====================================================
    // 409 - Out of stock
    // =====================================================
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ApiErrorResponse> handleOutOfStock(
            OutOfStockException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(409, "OUT_OF_STOCK", ex.getMessage(), request.getRequestURI())
        );
    }

    // =====================================================
    // 500 - Catch-all
    // =====================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected API error at URI: {} | {}: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiErrorResponse(500, "INTERNAL_ERROR",
                        "Đã xảy ra lỗi hệ thống", request.getRequestURI())
        );
    }
}

package com.natk.natk_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        int status = resolveStatus(ex);
        String error = resolveErrorMessage(status);

        return buildErrorResponse(ex.getMessage(), error, status, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Forbidden", HttpServletResponse.SC_FORBIDDEN, request);
    }


    private int resolveStatus(IllegalArgumentException ex) {
        return "User not found".equals(ex.getMessage())
                ? HttpServletResponse.SC_NOT_FOUND
                : HttpServletResponse.SC_UNAUTHORIZED;
    }

    private String resolveErrorMessage(int status) {
        return switch (status) {
            case HttpServletResponse.SC_NOT_FOUND -> "Not Found";
            case HttpServletResponse.SC_UNAUTHORIZED -> "Unauthorized";
            default -> "Error";
        };
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message,
                                                             String error,
                                                             int status,
                                                             HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}

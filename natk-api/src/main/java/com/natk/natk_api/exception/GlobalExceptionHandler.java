package com.natk.natk_api.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.natk.natk_api.llms.FileConversionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        int status = resolveStatus(ex);
        String error = resolveErrorMessage(status);

        return buildErrorResponse(ex.getMessage(), error, status, request);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, JsonProcessingException.class })
    public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        return buildErrorResponse("Invalid request body: " + ex.getMostSpecificCause().getMessage(),
                "Bad Request",
                HttpServletResponse.SC_BAD_REQUEST,
                request);
    }

    @ExceptionHandler(FileOrFolderNotFoundOrNoAccessException.class)
    public ResponseEntity<ErrorResponse> handleFileOrFolderNotFound(FileOrFolderNotFoundOrNoAccessException ex,
                                                                    HttpServletRequest request) {
        return buildErrorResponse(
                ex.getMessage(),
                "Not Found",
                HttpServletResponse.SC_NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex.getMessage(), "Forbidden", HttpServletResponse.SC_FORBIDDEN, request);
    }

    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateName(DuplicateNameException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("suggestedName", ex.getSuggestedName());
        return ResponseEntity.badRequest().body(response);
    }

    private int resolveStatus(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null) return HttpServletResponse.SC_BAD_REQUEST;

        if (message.contains("User not found")) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else if (message.contains("File with the same name already exists")) {
            return HttpServletResponse.SC_CONFLICT;
        }

        return HttpServletResponse.SC_BAD_REQUEST;
    }

    @ExceptionHandler(FileConversionException.class)
    public ResponseEntity<FileConversionErrorResponse> handleFileConversion(FileConversionException ex) {
        FileConversionErrorResponse body = new FileConversionErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage(),
                ex.getFailedFiles()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private String resolveErrorMessage(int status) {
        return switch (status) {
            case HttpServletResponse.SC_NOT_FOUND -> "Not Found";
            case HttpServletResponse.SC_UNAUTHORIZED -> "Unauthorized";
            case HttpServletResponse.SC_CONFLICT -> "Conflict";
            case HttpServletResponse.SC_FORBIDDEN -> "Forbidden";
            case HttpServletResponse.SC_BAD_REQUEST -> "Bad Request";
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

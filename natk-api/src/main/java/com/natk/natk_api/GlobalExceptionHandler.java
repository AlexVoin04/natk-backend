package com.natk.natk_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        if ("User not found".equals(ex.getMessage())) {
            return ResponseEntity.status(404).body(ex.getMessage());
        }
        return ResponseEntity.status(401).body(ex.getMessage());
    }
}

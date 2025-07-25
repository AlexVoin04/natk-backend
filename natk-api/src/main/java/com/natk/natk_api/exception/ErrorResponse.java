package com.natk.natk_api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public static void writeToResponse(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String error,
                                       String message) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(response.getStatus())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(response.getStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

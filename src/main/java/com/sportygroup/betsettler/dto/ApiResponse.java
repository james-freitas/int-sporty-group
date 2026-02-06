package com.sportygroup.betsettler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {

    /**
     * Response message
     */
    private String message;

    /**
     * Event ID (if applicable)
     */
    private String eventId;

    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code
     */
    private Integer status;

    /**
     * Creates a success response
     */
    public static ApiResponse success(String message, String eventId) {
        return ApiResponse.builder()
                .message(message)
                .eventId(eventId)
                .status(202)
                .build();
    }

    /**
     * Creates an error response
     */
    public static ApiResponse error(String message, Integer status) {
        return ApiResponse.builder()
                .message(message)
                .status(status)
                .build();
    }
}
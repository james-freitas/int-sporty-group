package com.sportygroup.betsettler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for publishing event outcomes via REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishEventRequest {

    /**
     * Unique identifier of the event
     */
    @NotBlank(message = "Event ID cannot be blank")
    private String eventId;

    /**
     * Name or description of the event
     */
    @NotBlank(message = "Event name cannot be blank")
    private String eventName;

    /**
     * Identifier of the winning team/player/outcome
     */
    @NotBlank(message = "Event winner ID cannot be blank")
    private String eventWinnerId;
}

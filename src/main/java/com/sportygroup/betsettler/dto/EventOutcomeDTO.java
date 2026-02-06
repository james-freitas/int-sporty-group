package com.sportygroup.betsettler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object for Event Outcome messages.
 *
 * Used for Kafka messaging when an event outcome is published.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOutcomeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier of the event
     */
    @NotBlank(message = "Event ID is required")
    @JsonProperty("eventId")
    private String eventId;

    /**
     * Name or description of the event
     */
    @NotBlank(message = "Event name is required")
    @JsonProperty("eventName")
    private String eventName;

    /**
     * Identifier of the winning team/player/outcome
     */
    @NotBlank(message = "Event winner ID is required")
    @JsonProperty("eventWinnerId")
    private String eventWinnerId;
}

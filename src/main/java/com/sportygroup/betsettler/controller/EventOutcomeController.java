package com.sportygroup.betsettler.controller;

import com.sportygroup.betsettler.dto.ApiResponse;
import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import com.sportygroup.betsettler.dto.PublishEventRequest;
import com.sportygroup.betsettler.service.EventOutcomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for event outcome operations.
 *
 * Provides API endpoint for publishing event outcomes to Kafka.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventOutcomeController {

    private final EventOutcomeService eventOutcomeService;

    /**
     * Publishes an event outcome to Kafka.
     *
     * POST /api/events/outcomes
     *
     * This endpoint accepts event outcome data and publishes it to Kafka
     * for asynchronous processing. The response is returned immediately
     * (202 Accepted) while bet matching and settlement occur asynchronously.
     *
     * @param request The event outcome request
     * @return 202 Accepted with success message
     */
    @PostMapping("/outcomes")
    public ResponseEntity<ApiResponse> publishEventOutcome(
            @Valid @RequestBody PublishEventRequest request) {

        log.info("Received request to publish event outcome - Event ID: {}, Event Name: {}, Winner: {}",
                request.getEventId(), request.getEventName(), request.getEventWinnerId());

        try {
            // Convert request to DTO
            EventOutcomeDTO eventOutcome = EventOutcomeDTO.builder()
                    .eventId(request.getEventId())
                    .eventName(request.getEventName())
                    .eventWinnerId(request.getEventWinnerId())
                    .build();

            // Publish to Kafka
            eventOutcomeService.publishEventOutcome(eventOutcome);

            log.info("Event outcome published successfully - Event ID: {}", request.getEventId());

            // Return 202 Accepted (async processing)
            ApiResponse response = ApiResponse.success(
                    "Event outcome published successfully",
                    request.getEventId()
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            log.error("Failed to publish event outcome - Event ID: {}, Error: {}",
                    request.getEventId(), e.getMessage(), e);

            ApiResponse errorResponse = ApiResponse.error(
                    "Failed to publish event outcome: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Health check endpoint for the controller.
     *
     * GET /api/events/health
     *
     * @return 200 OK with status message
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        ApiResponse response = ApiResponse.builder()
                .message("Event outcome service is running")
                .status(HttpStatus.OK.value())
                .build();

        return ResponseEntity.ok(response);
    }
}
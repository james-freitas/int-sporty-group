package com.sportygroup.betsettler.controller;

import com.sportygroup.betsettler.dto.EventOutcomeDTO;
import com.sportygroup.betsettler.dto.PublishEventRequest;
import com.sportygroup.betsettler.service.EventOutcomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EventOutcomeController.
 */
@WebMvcTest(EventOutcomeController.class)
class EventOutcomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventOutcomeService eventOutcomeService;

    @Test
    void publishEventOutcome_WithValidRequest_Returns202() throws Exception {
        // Given
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-001")
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        doNothing().when(eventOutcomeService).publishEventOutcome(any(EventOutcomeDTO.class));

        // When & Then
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", is("Event outcome published successfully")))
                .andExpect(jsonPath("$.eventId", is("EVT-001")))
                .andExpect(jsonPath("$.status", is(202)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(eventOutcomeService).publishEventOutcome(any(EventOutcomeDTO.class));
    }

    @Test
    void publishEventOutcome_WithMissingEventId_Returns400() throws Exception {
        // Given
        PublishEventRequest request = PublishEventRequest.builder()
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        // When & Then
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors.eventId", notNullValue()));

        verify(eventOutcomeService, never()).publishEventOutcome(any());
    }

    @Test
    void publishEventOutcome_WithMissingEventName_Returns400() throws Exception {
        // Given
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-001")
                .eventWinnerId("TEAM-A")
                .build();

        // When & Then
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventName", notNullValue()));

        verify(eventOutcomeService, never()).publishEventOutcome(any());
    }

    @Test
    void publishEventOutcome_WithMissingWinnerId_Returns400() throws Exception {
        // Given
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-001")
                .eventName("Test Match")
                .build();

        // When & Then
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventWinnerId", notNullValue()));

        verify(eventOutcomeService, never()).publishEventOutcome(any());
    }

    @Test
    void publishEventOutcome_WithBlankEventId_Returns400() throws Exception {
        // Given
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("")
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        // When & Then
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(eventOutcomeService, never()).publishEventOutcome(any());
    }

    @Test
    void publishEventOutcome_WithServiceException_Returns500() throws Exception {
        // Given
        PublishEventRequest request = PublishEventRequest.builder()
                .eventId("EVT-001")
                .eventName("Test Match")
                .eventWinnerId("TEAM-A")
                .build();

        doThrow(new RuntimeException("Kafka connection failed"))
                .when(eventOutcomeService).publishEventOutcome(any(EventOutcomeDTO.class));

        // When & Then
        mockMvc.perform(post("/api/events/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", containsString("Failed to publish event outcome")));
    }

    @Test
    void health_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/events/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Event outcome service is running")))
                .andExpect(jsonPath("$.status", is(200)));
    }
}
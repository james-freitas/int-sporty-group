package com.sportygroup.betsettler.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WelcomeController.
 *
 * Tests the welcome endpoint and favicon endpoint including:
 * - Response structure and content
 * - HTTP status codes
 * - JSON structure validation
 * - All expected fields present
 * - Correct endpoint information
 */
@WebMvcTest(WelcomeController.class)
class WelcomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void welcome_ReturnsOkStatus() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void welcome_ReturnsApplicationName() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.application", is("Sports Betting Settlement Service")));
    }

    @Test
    void welcome_ReturnsVersion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }

    @Test
    void welcome_ReturnsRunningStatus() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.status", is("running")));
    }

    @Test
    void welcome_ReturnsEndpointsMap() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints", is(notNullValue())))
                .andExpect(jsonPath("$.endpoints", isA(Map.class)));
    }

    @Test
    void welcome_ContainsPublishEventOutcomeEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Publish Event Outcome']",
                        is("POST /api/events/outcomes")));
    }

    @Test
    void welcome_ContainsHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Health Check']",
                        is("GET /api/events/health")));
    }

    @Test
    void welcome_ContainsH2ConsoleEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['H2 Console']",
                        is("GET /h2-console")));
    }

    @Test
    void welcome_ContainsActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Actuator Health']",
                        is("GET /actuator/health")));
    }

    @Test
    void welcome_ContainsActuatorMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Actuator Metrics']",
                        is("GET /actuator/metrics")));
    }

    @Test
    void welcome_ContainsAllFiveEndpoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints", aMapWithSize(5)))
                .andExpect(jsonPath("$.endpoints", hasKey("Publish Event Outcome")))
                .andExpect(jsonPath("$.endpoints", hasKey("Health Check")))
                .andExpect(jsonPath("$.endpoints", hasKey("H2 Console")))
                .andExpect(jsonPath("$.endpoints", hasKey("Actuator Health")))
                .andExpect(jsonPath("$.endpoints", hasKey("Actuator Metrics")));
    }

    @Test
    void welcome_ReturnsDocumentationMap() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation", is(notNullValue())))
                .andExpect(jsonPath("$.documentation", isA(Map.class)));
    }

    @Test
    void welcome_ContainsReadmeDocumentation() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation.README",
                        is("See README.md for complete documentation")));
    }

    @Test
    void welcome_ContainsQuickReferenceDocumentation() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation['Quick Reference']",
                        is("See QUICK_REFERENCE.md for common commands")));
    }

    @Test
    void welcome_ContainsH2ConsoleGuideDocumentation() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation['H2 Console Guide']",
                        is("See H2_CONSOLE_GUIDE.md for database access")));
    }

    @Test
    void welcome_ContainsAllThreeDocumentationEntries() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation", aMapWithSize(3)))
                .andExpect(jsonPath("$.documentation", hasKey("README")))
                .andExpect(jsonPath("$.documentation", hasKey("Quick Reference")))
                .andExpect(jsonPath("$.documentation", hasKey("H2 Console Guide")));
    }

    @Test
    void welcome_ReturnsCompleteResponseStructure() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$", hasKey("application")))
                .andExpect(jsonPath("$", hasKey("version")))
                .andExpect(jsonPath("$", hasKey("status")))
                .andExpect(jsonPath("$", hasKey("endpoints")))
                .andExpect(jsonPath("$", hasKey("documentation")));
    }

    @Test
    void welcome_ReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void welcome_AllEndpointValuesAreStrings() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Publish Event Outcome']", isA(String.class)))
                .andExpect(jsonPath("$.endpoints['Health Check']", isA(String.class)))
                .andExpect(jsonPath("$.endpoints['H2 Console']", isA(String.class)))
                .andExpect(jsonPath("$.endpoints['Actuator Health']", isA(String.class)))
                .andExpect(jsonPath("$.endpoints['Actuator Metrics']", isA(String.class)));
    }

    @Test
    void welcome_AllDocumentationValuesAreStrings() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation.README", isA(String.class)))
                .andExpect(jsonPath("$.documentation['Quick Reference']", isA(String.class)))
                .andExpect(jsonPath("$.documentation['H2 Console Guide']", isA(String.class)));
    }

    @Test
    void welcome_ApplicationFieldIsString() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.application", isA(String.class)))
                .andExpect(jsonPath("$.application", not(emptyString())));
    }

    @Test
    void welcome_VersionFieldIsString() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.version", isA(String.class)))
                .andExpect(jsonPath("$.version", matchesPattern("\\d+\\.\\d+\\.\\d+")));
    }

    @Test
    void welcome_StatusFieldIsString() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.status", isA(String.class)))
                .andExpect(jsonPath("$.status", not(emptyString())));
    }

    @Test
    void welcome_EndpointsContainHttpMethods() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Publish Event Outcome']", containsString("POST")))
                .andExpect(jsonPath("$.endpoints['Health Check']", containsString("GET")))
                .andExpect(jsonPath("$.endpoints['H2 Console']", containsString("GET")))
                .andExpect(jsonPath("$.endpoints['Actuator Health']", containsString("GET")))
                .andExpect(jsonPath("$.endpoints['Actuator Metrics']", containsString("GET")));
    }

    @Test
    void welcome_EndpointsContainPaths() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Publish Event Outcome']", containsString("/api/events/outcomes")))
                .andExpect(jsonPath("$.endpoints['Health Check']", containsString("/api/events/health")))
                .andExpect(jsonPath("$.endpoints['H2 Console']", containsString("/h2-console")))
                .andExpect(jsonPath("$.endpoints['Actuator Health']", containsString("/actuator/health")))
                .andExpect(jsonPath("$.endpoints['Actuator Metrics']", containsString("/actuator/metrics")));
    }

    @Test
    void welcome_MultipleCalls_ReturnConsistentResponse() throws Exception {
        // First call
        String response1 = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Second call
        String response2 = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Both responses should be identical
        assert response1.equals(response2);
    }

    @Test
    void welcome_NoQueryParameters_ReturnsSuccessfully() throws Exception {
        mockMvc.perform(get("/")
                        .queryParam("someParam", "someValue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application", is("Sports Betting Settlement Service")));
    }

    @Test
    void favicon_ReturnsOkStatus() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk());
    }

    @Test
    void favicon_ReturnsEmptyResponse() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(content().string(""));
    }

    @Test
    void favicon_DoesNotReturnJson() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void favicon_AcceptsGetRequest() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk());
    }

    @Test
    void favicon_WithQueryParameters_StillReturnsOk() throws Exception {
        mockMvc.perform(get("/favicon.ico")
                        .queryParam("test", "value"))
                .andExpect(status().isOk());
    }

    @Test
    void favicon_MultipleCalls_AlwaysReturnsOk() throws Exception {
        mockMvc.perform(get("/favicon.ico")).andExpect(status().isOk());
        mockMvc.perform(get("/favicon.ico")).andExpect(status().isOk());
        mockMvc.perform(get("/favicon.ico")).andExpect(status().isOk());
    }

    @Test
    void welcome_WithTrailingSlash_ReturnsOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application", is("Sports Betting Settlement Service")));
    }

    @Test
    void welcome_ResponseDoesNotContainNullValues() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.application", is(notNullValue())))
                .andExpect(jsonPath("$.version", is(notNullValue())))
                .andExpect(jsonPath("$.status", is(notNullValue())))
                .andExpect(jsonPath("$.endpoints", is(notNullValue())))
                .andExpect(jsonPath("$.documentation", is(notNullValue())));
    }

    @Test
    void welcome_ResponseDoesNotContainEmptyStrings() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.application", not(emptyString())))
                .andExpect(jsonPath("$.version", not(emptyString())))
                .andExpect(jsonPath("$.status", not(emptyString())));
    }

    @Test
    void welcome_NestedMapsAreNotEmpty() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints", not(anEmptyMap())))
                .andExpect(jsonPath("$.documentation", not(anEmptyMap())));
    }

    @Test
    void welcome_AllEndpointKeysExist() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.endpoints['Publish Event Outcome']", notNullValue()))
                .andExpect(jsonPath("$.endpoints['Health Check']", notNullValue()))
                .andExpect(jsonPath("$.endpoints['H2 Console']", notNullValue()))
                .andExpect(jsonPath("$.endpoints['Actuator Health']", notNullValue()))
                .andExpect(jsonPath("$.endpoints['Actuator Metrics']", notNullValue()));
    }

    @Test
    void welcome_AllDocumentationKeysExist() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$.documentation.README", notNullValue()))
                .andExpect(jsonPath("$.documentation['Quick Reference']", notNullValue()))
                .andExpect(jsonPath("$.documentation['H2 Console Guide']", notNullValue()));
    }

    @Test
    void welcome_ResponseIsValidJson() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void welcome_ResponseCanBeParsedAsMap() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(jsonPath("$", isA(Map.class)));
    }
}
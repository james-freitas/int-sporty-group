package com.sportygroup.betsettler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Welcome controller for root endpoint.
 *
 * Provides information about available endpoints when accessing the root URL.
 */
@Controller
public class WelcomeController {

    /**
     * Root endpoint - provides API information.
     */
    @GetMapping("/")
    @ResponseBody
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Sports Betting Settlement Service");
        response.put("version", "1.0.0");
        response.put("status", "running");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("Publish Event Outcome", "POST /api/events/outcomes");
        endpoints.put("Health Check", "GET /api/events/health");
        endpoints.put("H2 Console", "GET /h2-console");
        endpoints.put("Actuator Health", "GET /actuator/health");
        endpoints.put("Actuator Metrics", "GET /actuator/metrics");

        response.put("endpoints", endpoints);

        Map<String, String> documentation = new HashMap<>();
        documentation.put("README", "See README.md for complete documentation");
        documentation.put("Quick Reference", "See QUICK_REFERENCE.md for common commands");
        documentation.put("H2 Console Guide", "See H2_CONSOLE_GUIDE.md for database access");

        response.put("documentation", documentation);

        return response;
    }

    /**
     * Favicon endpoint - prevents 404 errors.
     * Returns empty response to avoid browser errors.
     */
    @GetMapping("/favicon.ico")
    @ResponseBody
    public void favicon() {
        // Empty response - prevents error logs
    }
}

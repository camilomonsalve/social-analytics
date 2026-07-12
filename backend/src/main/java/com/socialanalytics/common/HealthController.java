package com.socialanalytics.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirms the API is reachable. This is the first endpoint the frontend
 * calls to verify the full pipeline (Angular -> Spring Boot -> Postgres) works.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public HealthStatus health() {
        return HealthStatus.up();
    }
}

package com.socialanalytics.common;

/**
 * Simple response payload for the health check endpoint.
 */
public record HealthStatus(String status) {

    public static HealthStatus up() {
        return new HealthStatus("UP");
    }
}

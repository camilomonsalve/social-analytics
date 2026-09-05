package com.socialanalytics.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sync")
public record SyncProperties(Csv csv, Scheduler scheduler) {
    public record Csv(String sourceUrl, int connectTimeoutMs, int readTimeoutMs) {}
    public record Scheduler(boolean enabled, String cron) {}
}

package com.socialanalytics.synchronization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 20)
    private TriggeredBy triggeredBy;

    @Column(name = "source_url", nullable = false, length = 1024)
    private String sourceUrl;

    @Column(name = "profiles_created", nullable = false)
    private int profilesCreated;

    @Column(name = "profiles_updated", nullable = false)
    private int profilesUpdated;

    @Column(name = "profiles_unchanged", nullable = false)
    private int profilesUnchanged;

    @Column(name = "profiles_failed", nullable = false)
    private int profilesFailed;

    @Column(name = "profiles_skipped_invalid", nullable = false)
    private int profilesSkippedInvalid;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}

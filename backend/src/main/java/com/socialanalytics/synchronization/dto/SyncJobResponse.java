package com.socialanalytics.synchronization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncJobResponse {
    private UUID id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime finishedAt;
    
    private String status;
    private String triggeredBy;
    private int profilesCreated;
    private int profilesUpdated;
    private int profilesUnchanged;
    private int profilesFailed;
    private int profilesSkippedInvalid;
    private String errorMessage;
}

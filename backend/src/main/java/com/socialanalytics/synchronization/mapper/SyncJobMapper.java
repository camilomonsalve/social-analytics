package com.socialanalytics.synchronization.mapper;

import com.socialanalytics.synchronization.dto.SyncJobResponse;
import com.socialanalytics.synchronization.entity.SyncJob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SyncJobMapper {

    SyncJobResponse toResponse(SyncJob syncJob);
}

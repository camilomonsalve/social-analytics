package com.socialanalytics.synchronization.repository;

import com.socialanalytics.synchronization.entity.SyncJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    Optional<SyncJob> findFirstByOrderByStartedAtDesc();
    List<SyncJob> findAllByOrderByStartedAtDesc(Pageable pageable);
}

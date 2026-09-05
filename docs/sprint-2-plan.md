# Sprint 2 — Automatic CSV Synchronization

**Branch:** `feature/csv-sync`
**Builds on:** Sprint 1 (`profile` module — merged into `main`)
**Roadmap reference:** `docs/roadmap.md` → Sprint 2 — "Scheduler que descarga el CSV periódicamente" + "Detección de cambios (hash por perfil) y actualización incremental"

---

## 0. Read first — do this before writing any code

1. Actually fetch `https://apoyaronaabelardo.org/datos.csv` and inspect the real headers, encoding, and a sample of rows. **Sprint 1 was only ever tested against `test-data.csv`, a controlled fixture — the real remote file has never been parsed end-to-end by this codebase.** If its column names, delimiter, or encoding differ from what `CsvImporter` currently expects (`nombre`, `descripcion`, `foto`, `categoria`), that mismatch needs to be resolved *before* building the downloader around it, not discovered after.
2. Re-read `backend/src/main/java/com/socialanalytics/profile/importer/CsvImporter.java`, `ProfileService.java`, and `ProfileRepository.java` in full before touching them — Sprint 2 modifies `ProfileService.upsertProfile` in a way that changes its return type and existing test expectations (see §3).
3. Note the pre-existing empty package placeholders from the Sprint 0 skeleton: `synchronization/csv/.gitkeep` and `synchronization/scheduler/.gitkeep`. This sprint fills those in, plus adds sibling packages (`entity`, `repository`, `dto`, `controller`, `service`, `exception`) mirroring the `profile` module's internal structure — the whole point of that original skeleton was to have this shape ready.

---

## 1. Scope

**In scope:**
- Download `datos.csv` from a configured remote URL on a schedule (and on-demand via an endpoint).
- Detect which profiles actually changed since the last sync (per-profile content hash, not whole-file diffing).
- Only write to the database for profiles that are new or changed; skip unchanged ones (no unnecessary `updated_at` bumps).
- Record the outcome of every sync run (created/updated/unchanged/failed counts, timing, errors) for observability.
- Expose a manual trigger endpoint and a history/status endpoint.

**Out of scope (do not build this sprint):**
- `SocialAccount` / `MetricSnapshot` entities or any social-media provider (Instagram, etc.) — that's Sprint 3.
- Any frontend UI for sync status. (A "last synced" indicator on the dashboard is a reasonable *future* addition, not part of this sprint's roadmap scope — skip it here.)
- Removing or changing the existing `POST /api/v1/profiles/import` endpoint from Sprint 1. It stays as-is (manual arbitrary-file upload, useful for testing). This sprint adds a *separate*, distinct sync path — see §5.

---

## 2. Architecture

```
                    @Scheduled (SyncScheduler)          POST /api/v1/sync/trigger (SyncController)
                              │                                          │
                              └───────────────┬──────────────────────────┘
                                               ▼
                                   SynchronizationService
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    ▼                          ▼                          ▼
             CsvDownloader              CsvImporter                 SyncJobRepository
          (HTTP GET → bytes)      (parse, reuses Sprint 1        (persist RUNNING →
                                    logic unchanged)              SUCCESS/FAILED record)
                                               │
                                               ▼
                                     ProfileService.upsertProfile
                                     (per-row, hash-aware — MODIFIED this sprint)
                                               │
                                               ▼
                                        ProfileRepository
```

Key design decision: **change detection lives on the `Profile` entity itself** (a `content_hash` column), not as a separate file-diffing step comparing today's CSV against yesterday's saved copy. Reasoning: it's simpler, requires no shared filesystem state (works fine if this ever runs across multiple instances), and reuses the exact same upsert path whether triggered by the scheduler or a manual `/import` upload.

---

## 3. Database changes

New Flyway migration: `backend/src/main/resources/db/migration/V2__add_sync_tracking.sql`

```sql
ALTER TABLE profile ADD COLUMN content_hash CHAR(64);

CREATE TABLE sync_job
(
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at              TIMESTAMP NOT NULL,
    finished_at             TIMESTAMP,
    status                  VARCHAR(20) NOT NULL,
    triggered_by            VARCHAR(20) NOT NULL,
    source_url              VARCHAR(1024) NOT NULL,
    profiles_created        INT NOT NULL DEFAULT 0,
    profiles_updated        INT NOT NULL DEFAULT 0,
    profiles_unchanged      INT NOT NULL DEFAULT 0,
    profiles_failed         INT NOT NULL DEFAULT 0,
    error_message           TEXT
);

CREATE INDEX idx_sync_job_started_at ON sync_job (started_at DESC);
```

Notes:
- `content_hash` is nullable on purpose. Existing Sprint 1 profiles have no hash yet — they don't need a backfill migration. The first time `upsertProfile` processes them again (next sync run), it'll compute and store a hash naturally, since a `null` stored hash never equals a freshly computed one, so it's treated as an update. Don't write a backfill script for this — let it happen organically.
- `status` values: `RUNNING`, `SUCCESS`, `FAILED`. `triggered_by` values: `SCHEDULED`, `MANUAL`. Model both as Java enums (see §4), stored via `@Enumerated(EnumType.STRING)` — not as raw strings scattered through the code.
- Never modify `V1__create_profile_table.sql`. This is a new file, `V2`.

---

## 4. New backend classes

### 4.1 `com.socialanalytics.synchronization.entity.SyncStatus` (enum)
```java
public enum SyncStatus {
    RUNNING, SUCCESS, FAILED
}
```

### 4.2 `com.socialanalytics.synchronization.entity.TriggeredBy` (enum)
```java
public enum TriggeredBy {
    SCHEDULED, MANUAL
}
```

### 4.3 `com.socialanalytics.synchronization.entity.SyncJob`
JPA entity mapping to `sync_job`. Fields: `id` (UUID), `startedAt`/`finishedAt` (`LocalDateTime` — match the type already used in `Profile.createdAt`/`updatedAt`, don't introduce `Instant` and mix time types across the codebase), `status` (`SyncStatus`), `triggeredBy` (`TriggeredBy`), `sourceUrl` (String), `profilesCreated`/`profilesUpdated`/`profilesUnchanged`/`profilesFailed` (int), `errorMessage` (String, nullable, `@Column(columnDefinition = "TEXT")`).

### 4.4 `com.socialanalytics.synchronization.repository.SyncJobRepository`
```java
public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    Optional<SyncJob> findFirstByOrderByStartedAtDesc();
    List<SyncJob> findAllByOrderByStartedAtDesc(Pageable pageable);
}
```

### 4.5 `com.socialanalytics.synchronization.exception.SyncException`
```java
public class SyncException extends RuntimeException {
    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
```
Thrown when the download or parse step fails entirely (not for individual bad CSV rows — those are handled per-row, see §4.8).

### 4.6 Configuration — `com.socialanalytics.configuration.SyncProperties`
Typed configuration, matching the project's established convention (no scattered `@Value`):
```java
@ConfigurationProperties(prefix = "sync")
public record SyncProperties(Csv csv, Scheduler scheduler) {
    public record Csv(String sourceUrl, int connectTimeoutMs, int readTimeoutMs) {}
    public record Scheduler(boolean enabled, String cron) {}
}
```
Add `@ConfigurationPropertiesScan` to `SocialAnalyticsApplication` (the main class) so this — and any future `@ConfigurationProperties` classes, e.g. for Sprint 3's providers — gets picked up automatically without individually listing `@EnableConfigurationProperties(...)` per class.

Add to `application.yml`:
```yaml
sync:
  csv:
    source-url: https://apoyaronaabelardo.org/datos.csv
    connect-timeout-ms: 5000
    read-timeout-ms: 15000
  scheduler:
    enabled: true
    cron: "0 0 */6 * * *"
```

### 4.7 `com.socialanalytics.synchronization.csv.CsvDownloader`
Downloads the raw CSV bytes from `SyncProperties.csv().sourceUrl()`. Use Spring's `RestClient` (available since Spring 6.1 / already part of `spring-boot-starter-web`, which the project already depends on) — don't add `RestTemplate` or `WebClient` as a new dependency, `RestClient` is the current idiomatic synchronous choice and needs nothing extra.

```java
@Component
@RequiredArgsConstructor
public class CsvDownloader {

    private final SyncProperties syncProperties;

    public InputStream download() {
        RestClient client = RestClient.builder()
                .requestFactory(clientRequestFactory())
                .build();
        try {
            byte[] body = client.get()
                    .uri(syncProperties.csv().sourceUrl())
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new SyncException("Downloaded CSV is empty", null);
            }
            return new ByteArrayInputStream(body);
        } catch (RestClientException e) {
            throw new SyncException("Failed to download CSV from " + syncProperties.csv().sourceUrl(), e);
        }
    }

    private ClientHttpRequestFactory clientRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(syncProperties.csv().connectTimeoutMs());
        factory.setReadTimeout(syncProperties.csv().readTimeoutMs());
        return factory;
    }
}
```
This is close to complete — implement it essentially as written, adjusting only if `RestClient`'s exact API in Spring Boot 4.1 differs from this (verify against the actual dependency versions in `pom.xml` if the build complains about a missing method).

### 4.8 `ProfileService` — MODIFY existing class

This is the most important, most bug-prone part of this sprint. Read it twice before implementing.

**Add a new enum**, `com.socialanalytics.profile.service.UpsertResult`:
```java
public enum UpsertResult {
    CREATED, UPDATED, UNCHANGED
}
```

**Change `upsertProfile`'s return type from `boolean` to `UpsertResult`**, and make it hash-aware:
```java
@Transactional
public UpsertResult upsertProfile(Profile incoming) {
    String newHash = computeContentHash(incoming);
    return profileRepository.findByNombre(incoming.getNombre())
            .map(existing -> {
                if (newHash.equals(existing.getContentHash())) {
                    return UpsertResult.UNCHANGED;
                }
                existing.setDescripcion(incoming.getDescripcion());
                existing.setFoto(incoming.getFoto());
                existing.setCategoria(incoming.getCategoria());
                existing.setContentHash(newHash);
                profileRepository.save(existing);
                return UpsertResult.UPDATED;
            })
            .orElseGet(() -> {
                incoming.setContentHash(newHash);
                profileRepository.save(incoming);
                return UpsertResult.CREATED;
            });
}

private String computeContentHash(Profile profile) {
    String raw = String.join("|",
            nullToEmpty(profile.getNombre()),
            nullToEmpty(profile.getDescripcion()),
            nullToEmpty(profile.getFoto()),
            nullToEmpty(profile.getCategoria())
    );
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 not available", e);
    }
}

private String nullToEmpty(String value) {
    return value != null ? value : "";
}
```

Important behavior: when the hash matches (`UNCHANGED`), **`profileRepository.save()` is never called** — no DB write, no `updated_at` bump, genuinely a no-op. This is the whole point of the feature; don't accidentally call `save()` unconditionally and only vary the return value, or you haven't actually built change detection.

**This is a breaking change to Sprint 1 code — follow through on all call sites:**
- `CsvImporter.saveProfiles()` currently does something with the old `boolean` return (or ignores it) — update it to work with `UpsertResult` and tally counts if it needs to (see §4.9 for whether this logic moves).
- `ProfileServiceTest.upsertProfileCreatesWhenNotExists` and `upsertProfileUpdatesWhenExists` currently assert against `boolean` (`assertFalse`/`assertTrue`) — update both to assert the `UpsertResult` enum value (`assertEquals(UpsertResult.CREATED, ...)` / `assertEquals(UpsertResult.UPDATED, ...)`).
- **Add a new test**, `upsertProfileReturnsUnchangedWhenContentIdentical` — call `upsertProfile` twice with identical field values, assert the second call returns `UNCHANGED` and that `profileRepository.save()` was invoked only once total (`verify(profileRepository, times(1)).save(any())` across both calls, or structure the test to check the second call specifically doesn't trigger a save).

### 4.9 `com.socialanalytics.synchronization.service.SynchronizationService`

The orchestrator. Full responsibility walkthrough:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SynchronizationService {

    private final CsvDownloader csvDownloader;
    private final CsvImporter csvImporter;
    private final ProfileService profileService;
    private final SyncJobRepository syncJobRepository;
    private final SyncProperties syncProperties;

    public SyncJob synchronize(TriggeredBy triggeredBy) {
        SyncJob job = startJob(triggeredBy);

        try {
            InputStream csvStream = csvDownloader.download();
            List<Profile> parsedProfiles = csvImporter.importFromCsv(csvStream);

            int created = 0, updated = 0, unchanged = 0, failed = 0;
            for (Profile profile : parsedProfiles) {
                try {
                    UpsertResult result = profileService.upsertProfile(profile);
                    switch (result) {
                        case CREATED -> created++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> unchanged++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to upsert profile '{}': {}", profile.getNombre(), e.getMessage());
                    failed++;
                }
            }

            return finishJobSuccess(job, created, updated, unchanged, failed);

        } catch (SyncException e) {
            log.error("Sync failed during download/parse: {}", e.getMessage(), e);
            return finishJobFailed(job, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected SyncJob startJob(TriggeredBy triggeredBy) {
        SyncJob job = SyncJob.builder()
                .startedAt(LocalDateTime.now())
                .status(SyncStatus.RUNNING)
                .triggeredBy(triggeredBy)
                .sourceUrl(syncProperties.csv().sourceUrl())
                .build();
        return syncJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected SyncJob finishJobSuccess(SyncJob job, int created, int updated, int unchanged, int failed) {
        job.setFinishedAt(LocalDateTime.now());
        job.setStatus(SyncStatus.SUCCESS);
        job.setProfilesCreated(created);
        job.setProfilesUpdated(updated);
        job.setProfilesUnchanged(unchanged);
        job.setProfilesFailed(failed);
        return syncJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected SyncJob finishJobFailed(SyncJob job, String errorMessage) {
        job.setFinishedAt(LocalDateTime.now());
        job.setStatus(SyncStatus.FAILED);
        job.setErrorMessage(errorMessage);
        return syncJobRepository.save(job);
    }
}
```

**Two design decisions here that are easy to get wrong — implement exactly as described:**

1. **No single wrapping transaction around the whole row loop.** Each `profileService.upsertProfile()` call is already independently `@Transactional` (inherited from Sprint 1/§4.8). The loop catches exceptions **per row** and continues — one malformed or DB-constraint-violating row increments `failed` and moves on; it does not abort the rest of the batch. This is a deliberate difference from Sprint 1's CSV importer (which does wrap its whole batch in one transaction and aborts on the first bad row) — that choice made sense for a synchronous, attended, human-triggered upload where "tell me immediately if something's wrong" is the right behavior. This sprint's sync runs unattended on a schedule; a single bad row in a 500-row file shouldn't silently discard 499 good updates. Don't "fix" this into matching Sprint 1's behavior — they're intentionally different for different reasons.

2. **`startJob`/`finishJobSuccess`/`finishJobFailed` use `Propagation.REQUIRES_NEW`.** This makes each `SyncJob` bookkeeping write its own independent transaction, committed immediately, regardless of what happens afterward in the main `synchronize()` method. Without this, if something later in the method threw an unexpected exception, the `SyncJob` row itself could get rolled back along with everything else — leaving no record that a sync even ran, let alone that it failed. The whole point of `SyncJob` is to be a reliable audit trail even when things go wrong, so its own persistence can't be allowed to depend on the outcome of the thing it's recording.

### 4.10 `com.socialanalytics.synchronization.scheduler.SyncScheduler`
```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sync.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {

    private final SynchronizationService synchronizationService;

    @Scheduled(cron = "${sync.scheduler.cron}")
    public void scheduledSync() {
        synchronizationService.synchronize(TriggeredBy.SCHEDULED);
    }
}
```
`@EnableScheduling` is already present on `SocialAnalyticsApplication` from Sprint 0 — no change needed there.

### 4.11 DTOs — `com.socialanalytics.synchronization.dto.SyncJobResponse`
Mirror the pattern already used by `ProfileResponse`/`CategoryResponse` (Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@JsonFormat` on the date fields):
```java
public class SyncJobResponse {
    private UUID id;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private String triggeredBy;
    private int profilesCreated;
    private int profilesUpdated;
    private int profilesUnchanged;
    private int profilesFailed;
    private String errorMessage;
}
```
Add a matching `com.socialanalytics.synchronization.mapper.SyncJobMapper` (MapStruct interface, same pattern as `ProfileMapper`) — `SyncJob` → `SyncJobResponse`.

### 4.12 `com.socialanalytics.synchronization.controller.SyncController`
```java
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Synchronization", description = "Manual sync trigger and history")
public class SyncController {

    private final SynchronizationService synchronizationService;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobMapper syncJobMapper;

    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger a CSV sync")
    public SyncJobResponse trigger() {
        SyncJob job = synchronizationService.synchronize(TriggeredBy.MANUAL);
        if (job.getStatus() == SyncStatus.FAILED) {
            throw new SyncException(job.getErrorMessage(), null);
        }
        return syncJobMapper.toResponse(job);
    }

    @GetMapping("/status")
    @Operation(summary = "Get the most recent sync job")
    public SyncJobResponse status() {
        return syncJobRepository.findFirstByOrderByStartedAtDesc()
                .map(syncJobMapper::toResponse)
                .orElseThrow(() -> new SyncException("No sync has run yet", null));
    }

    @GetMapping("/history")
    @Operation(summary = "Get recent sync job history")
    public List<SyncJobResponse> history(@RequestParam(defaultValue = "20") int limit) {
        return syncJobRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit)).stream()
                .map(syncJobMapper::toResponse)
                .toList();
    }
}
```
Note: rethrowing `SyncException` from `trigger()` when the job failed means `/trigger` reports the failure back to whoever called it manually, rather than silently returning a 200 with a `FAILED` body buried inside. The scheduled path (`SyncScheduler`) doesn't have this problem since nothing's waiting on an HTTP response — it just logs, per §4.10.

### 4.13 `GlobalExceptionHandler` — add one handler
```java
@ExceptionHandler(SyncException.class)
public ResponseEntity<ErrorResponse> handleSyncException(SyncException ex) {
    ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_GATEWAY.value())
            .error("Bad Gateway")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
}
```
Use `502 Bad Gateway`, not 500 — the failure is specifically "the upstream CSV source is unreachable/broken," which is exactly what 502 communicates, and is consistent with the project's established pattern (Sprint 1: don't let every failure collapse into a generic 500 — see `ProfileNotFoundException` → 404 vs `IllegalArgumentException` → 400).

---

## 5. Relationship to the existing `/api/v1/profiles/import` endpoint

Keep it exactly as Sprint 1 built it — manual arbitrary-file upload, always upserts via `ProfileService.upsertProfile` (which is now hash-aware too, so re-uploading the same file via this endpoint will *also* correctly report/skip unchanged rows as a side effect — that's fine and expected, not a regression).

`POST /api/v1/sync/trigger` is a distinct, separate endpoint: it always downloads from the *configured remote URL*, not an uploaded file, and it records a `SyncJob`. Don't merge these two endpoints or make one call the other.

---

## 6. Tests required

- **`CsvDownloaderTest`** — mock the HTTP layer (Spring's `MockRestServiceServer` works well with `RestClient`). Cover: successful download, non-200 response, timeout, empty body.
- **`ProfileServiceTest`** — update the two existing upsert tests for the new `UpsertResult` return type (§4.8), add `upsertProfileReturnsUnchangedWhenContentIdentical`.
- **`SynchronizationServiceTest`** — mock `CsvDownloader`, `CsvImporter`, `ProfileService`, `SyncJobRepository`. Cover:
    - Full success path: verify counts (created/updated/unchanged) are tallied correctly and the final `SyncJob` has `status=SUCCESS`.
    - Download failure: verify `SyncJob` ends up `status=FAILED` with the error message captured, and that the method doesn't throw an unhandled exception up to the caller unexpectedly (the scheduled path must not crash the scheduler thread — confirm `synchronize()` returns normally even on failure, doesn't propagate).
    - One row failing mid-batch: verify it's counted in `profilesFailed`, and that the *other* rows in the same batch still get processed (this is the test that proves §4.9's "no wrapping transaction" design decision actually works as intended).
- **`SyncControllerTest`** — `/trigger` success (200 with correct body), `/trigger` when the underlying sync fails (502), `/status` with no prior runs (verify this is a sensible error, not an unhandled crash), `/status` with a prior run, `/history` returns results in descending `startedAt` order.
- **`SyncSchedulerTest`** — a simple test confirming `scheduledSync()` delegates to `synchronizationService.synchronize(TriggeredBy.SCHEDULED)`. Don't try to test actual cron timing.

---

## 7. Documentation to update

- `docs/roadmap.md` — check off Sprint 2, matching the existing format from Sprint 0/1.
- `docs/decisions.md` — add:
    - **ADR-009**: Hash-based per-profile change detection (stored as a `content_hash` column on `Profile`) instead of comparing successive CSV file snapshots. Rationale: simpler, no shared-filesystem dependency, reuses the same upsert path for both manual and scheduled imports.
    - **ADR-010**: `SyncJob` audit table for observability, with `REQUIRES_NEW` propagation so job bookkeeping survives independently of the sync operation's own success/failure.
    - **ADR-011**: Per-row error handling (not whole-batch transactional rollback) for scheduled syncs, deliberately different from Sprint 1's importer, because an unattended job shouldn't discard an entire batch over one bad row.

---

## 8. Manual verification checklist (for after implementation)

Once it's running locally, work through this before opening a PR — same spirit as Sprint 1's manual pass:

1. `GET /api/v1/sync/status` with no prior sync — should be a clean error response, not a 500 stack trace.
2. `POST /api/v1/sync/trigger` — first run against the real `apoyaronaabelardo.org/datos.csv`. Confirm it actually reaches the site (this is the first time this codebase has ever really talked to that URL — don't be surprised if headers/encoding need adjusting).
3. Check `GET /api/v1/profiles` — confirm the real data landed correctly, categories look sane.
4. `POST /api/v1/sync/trigger` again immediately, with no changes on the source. Confirm the response shows `profilesUpdated: 0` and `profilesUnchanged` equal to the total profile count — this is the core feature; if this doesn't hold, something's wrong with the hash comparison.
5. Manually edit one field of one profile directly in Postgres (e.g. via pgAdmin), then trigger sync again — confirm that one profile shows up as `updated`, not `unchanged` (proves the hash correctly detects real changes) — but also confirm it gets *overwritten back* to match the CSV (proves the CSV is the source of truth, not the DB).
6. `GET /api/v1/sync/history` — confirm multiple runs show up, most recent first.
7. Temporarily point `sync.csv.source-url` at an invalid URL, restart, trigger manually — confirm you get a 502 with a sensible message, and `GET /api/v1/sync/status` shows `status: FAILED` with the error captured, not a crash.
8. Set `sync.scheduler.cron` to something firing every minute temporarily (e.g. `0 * * * * *`), confirm it actually fires automatically without any manual trigger — then set it back to the real interval before committing.

---

## 9. Open questions to flag back during review

- Does the real CSV's actual schema match what `CsvImporter` expects? (See §0.) If not, note exactly what changed and how it was reconciled.
- What sync interval actually makes sense given how often the source site updates? `0 0 */6 * * *` (every 6 hours) is a reasonable placeholder from the original architecture discussion, not a hard requirement — flag if a different interval seems more appropriate once you've seen how the real data behaves.
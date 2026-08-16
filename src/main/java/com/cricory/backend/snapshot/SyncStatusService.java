package com.cricory.backend.snapshot;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SyncStatusService {
    private final Map<String, Status> statuses = new ConcurrentHashMap<>();

    public void success(String key, int itemCount) {
        statuses.compute(key, (ignored, previous) -> new Status(
                OffsetDateTime.now(),
                previous == null ? null : previous.lastFailureAt(),
                itemCount,
                0,
                ""));
    }

    public void failure(String key, RuntimeException exception) {
        statuses.compute(key, (ignored, previous) -> new Status(
                previous == null ? null : previous.lastSuccessAt(),
                OffsetDateTime.now(),
                previous == null ? 0 : previous.itemCount(),
                previous == null ? 1 : previous.consecutiveFailures() + 1,
                safeMessage(exception)));
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        statuses.forEach((key, status) -> result.put(key, status));
        return result;
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record Status(OffsetDateTime lastSuccessAt, OffsetDateTime lastFailureAt,
                         int itemCount, int consecutiveFailures, String lastError) { }
}

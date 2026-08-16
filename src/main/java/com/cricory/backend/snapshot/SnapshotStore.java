package com.cricory.backend.snapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class SnapshotStore {
    private final ApiSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    public SnapshotStore(ApiSnapshotRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void save(String key, Object value, String source) {
        String payload = objectMapper.writeValueAsString(value);
        ApiSnapshotEntity entity = repository.findById(key)
                .orElseGet(() -> new ApiSnapshotEntity(key, payload, source));
        entity.replace(payload, source);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public boolean exists(String key) { return repository.existsById(key); }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String key) {
        Object value = read(key);
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Map<String, Object> map(String key) {
        Object value = read(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> metadata(String key) {
        return repository.findById(key)
                .<Map<String, Object>>map(entity -> Map.of(
                        "source", entity.getSource(),
                        "fetchedAt", entity.getFetchedAt().toString(),
                        "updatedAt", entity.getUpdatedAt().toString()))
                .orElseGet(Map::of);
    }

    private Object read(String key) {
        return repository.findById(key).map(entity -> {
            try { return objectMapper.readValue(entity.getPayload(), Object.class); }
            catch (Exception exception) { throw new IllegalStateException("Invalid database snapshot: " + key, exception); }
        }).orElse(List.of());
    }
}

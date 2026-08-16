package com.cricory.backend.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "api_snapshots")
public class ApiSnapshotEntity {
    @Id
    @Column(name = "snapshot_key")
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String source;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ApiSnapshotEntity() { }

    public ApiSnapshotEntity(String key, String payload, String source) {
        this.key = key;
        replace(payload, source);
    }

    public void replace(String payload, String source) {
        this.payload = payload;
        this.source = source;
        this.fetchedAt = OffsetDateTime.now();
        this.updatedAt = this.fetchedAt;
    }

    public String getPayload() { return payload; }
    public String getSource() { return source; }
    public OffsetDateTime getFetchedAt() { return fetchedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

package com.cricory.backend.match;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "matches")
public class CricketMatchEntity {
    @Id
    private String id;
    @Column(name = "series_id") private String seriesId;
    private String name;
    @Column(name = "match_type") private String matchType;
    private String category;
    private String status;
    private String venue;
    @Column(name = "start_time") private OffsetDateTime startTime;
    @Column(name = "detail_link") private String detailLink;
    @Column(name = "source_payload") private String sourcePayload;
    @Column(name = "last_synced_at") private OffsetDateTime lastSyncedAt;
    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;

    protected CricketMatchEntity() { }

    public CricketMatchEntity(String id, String name, String matchType, String category,
                              String status, String venue, String detailLink, String sourcePayload) {
        this.id = id;
        this.name = name;
        this.matchType = matchType;
        this.category = category;
        this.status = status;
        this.venue = venue;
        this.detailLink = detailLink;
        this.sourcePayload = sourcePayload;
        this.lastSyncedAt = OffsetDateTime.now();
        this.createdAt = this.lastSyncedAt;
        this.updatedAt = this.lastSyncedAt;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public void refresh(String name, String matchType, String category, String status,
                        String venue, String detailLink, String sourcePayload) {
        this.name = name;
        this.matchType = matchType;
        this.category = category;
        this.status = status;
        this.venue = venue;
        this.detailLink = detailLink;
        this.sourcePayload = sourcePayload;
        this.lastSyncedAt = OffsetDateTime.now();
        this.updatedAt = this.lastSyncedAt;
    }
}

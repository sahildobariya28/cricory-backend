package com.cricory.backend.scraping;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import com.cricory.backend.snapshot.SnapshotKeys;
import com.cricory.backend.snapshot.SnapshotStore;

@RestController
@RequestMapping("/api")
@ConditionalOnProperty(prefix = "cricory.scraping", name = "enabled", havingValue = "true")
public class CricinfoController {

    private final SnapshotStore snapshotStore;

    public CricinfoController(SnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    @GetMapping("/live-matches")
    public ResponseEntity<Map<String, Object>> liveMatches() {
        return ResponseEntity.ok(Map.of("data", snapshotStore.list(SnapshotKeys.LIVE)));
    }

    @GetMapping("/upcoming-matches")
    public ResponseEntity<List<Map<String, Object>>> upcomingMatches() {
        return ResponseEntity.ok(snapshotStore.list(SnapshotKeys.UPCOMING));
    }

    @GetMapping("/recent-matches")
    public ResponseEntity<List<Map<String, Object>>> recentMatches() {
        return ResponseEntity.ok(snapshotStore.list(SnapshotKeys.RECENT));
    }
}

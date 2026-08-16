package com.cricory.backend.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final com.cricory.backend.snapshot.SnapshotStore snapshotStore;

    public CatalogController(com.cricory.backend.snapshot.SnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    @GetMapping("/news")
    public Map<String, Object> news() {
        return Map.of("data", snapshotStore.list(com.cricory.backend.snapshot.SnapshotKeys.NEWS));
    }

    @GetMapping("/series-list")
    public Map<String, Object> seriesList() {
        return nested(snapshotStore.list(com.cricory.backend.snapshot.SnapshotKeys.SERIES));
    }

    @GetMapping("/players-list")
    public Map<String, Object> playersList() {
        return nested(snapshotStore.list(com.cricory.backend.snapshot.SnapshotKeys.PLAYERS));
    }

    private Map<String, Object> nested(List<Map<String, Object>> items) {
        return Map.of("data", Map.of("data", items));
    }
}

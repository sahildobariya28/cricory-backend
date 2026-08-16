package com.cricory.backend.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        String key = com.cricory.backend.snapshot.SnapshotKeys.NEWS;
        return Map.of("data", snapshotStore.list(key), "meta", snapshotStore.metadata(key));
    }

}

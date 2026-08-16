package com.cricory.backend.snapshot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SyncStatusController {
    private final SyncStatusService statusService;

    public SyncStatusController(SyncStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/sync-status")
    public Map<String, Object> status() {
        return Map.of("data", statusService.snapshot());
    }
}

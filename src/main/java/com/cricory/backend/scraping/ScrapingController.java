package com.cricory.backend.scraping;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scraping")
@ConditionalOnProperty(prefix = "cricory.scraping", name = "enabled", havingValue = "true")
public class ScrapingController {

    private final WebScrapingService scrapingService;

    public ScrapingController(WebScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    @GetMapping("/preview")
    public ResponseEntity<ScrapeResult> preview() {
        return ResponseEntity.ok(scrapingService.scrapeConfiguredPage());
    }
}

package com.cricory.backend.scraping;

import java.time.Instant;

public record ScrapeResult(
        String requestedUrl,
        String finalUrl,
        String title,
        String visibleText,
        Instant scrapedAt
) {
}

package com.cricory.backend.snapshot;

import com.cricory.backend.catalog.NewsScrapingService;
import com.cricory.backend.catalog.NewsDataResult;
import com.cricory.backend.scraping.CricinfoScrapingService;
import com.cricory.backend.scraping.MatchDataResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ContentSnapshotSyncService {
    private final CricinfoScrapingService matchScraper;
    private final NewsScrapingService newsScraper;
    private final SnapshotStore store;

    public ContentSnapshotSyncService(CricinfoScrapingService matchScraper,
                                      NewsScrapingService newsScraper,
                                      SnapshotStore store) {
        this.matchScraper = matchScraper;
        this.newsScraper = newsScraper;
        this.store = store;
    }

    @Scheduled(initialDelayString = "20s", fixedDelayString = "${cricory.sync.live:20s}")
    public void syncLive() {
        safely(() -> persistMatchResult(SnapshotKeys.LIVE, matchScraper.liveMatchesResult()));
    }

    @Scheduled(initialDelayString = "30s", fixedDelayString = "${cricory.sync.upcoming:30m}")
    public void syncUpcoming() {
        safely(() -> persistMatchResult(SnapshotKeys.UPCOMING, matchScraper.upcomingMatchesResult()));
    }

    @Scheduled(initialDelayString = "40s", fixedDelayString = "${cricory.sync.recent:15m}")
    public void syncRecent() {
        safely(() -> persistMatchResult(SnapshotKeys.RECENT, matchScraper.recentMatchesResult()));
    }

    @Scheduled(initialDelayString = "50s", fixedDelayString = "${cricory.sync.news:10m}")
    public void syncNews() {
        safely(() -> persistNewsResult(newsScraper.latestNewsResult()));
    }

    @Scheduled(initialDelayString = "1m", fixedDelayString = "${cricory.sync.catalogs:24h}")
    public void seedCatalogs() {
        safely(() -> store.save(SnapshotKeys.SERIES, series(), "CATALOG_SEED"));
        safely(() -> store.save(SnapshotKeys.PLAYERS, players(), "CATALOG_SEED"));
    }

    private void safely(Runnable task) {
        try { task.run(); }
        catch (RuntimeException ignored) {
            // Never erase the last successful database snapshot on a scrape failure.
        }
    }

    private void persistMatchResult(String key, MatchDataResult result) {
        if (result.remoteSuccess()) {
            store.save(key, result.items(), result.source());
        } else if (!store.exists(key)) {
            store.save(key, result.items(), result.source());
        }
    }

    private void persistNewsResult(NewsDataResult result) {
        if (result.remoteSuccess()) {
            store.save(SnapshotKeys.NEWS, result.items(), result.source());
        } else if (!store.exists(SnapshotKeys.NEWS)) {
            store.save(SnapshotKeys.NEWS, result.items(), result.source());
        }
    }

    private List<Map<String, Object>> series() {
        return List.of(
                series("1543999", "India in Sri Lanka 2026", "2026-08-15", "2026-09-02", 3, 3, 2),
                series("1527258", "Bangladesh in Australia 2026", "2026-08-10", "2026-08-28", 3, 0, 2),
                series("1521176", "The Hundred Men's Competition 2026", "2026-07-20", "2026-08-14", 0, 34, 0),
                series("1544100", "New Zealand in England 2026", "2026-08-16", "2026-09-10", 5, 3, 0));
    }

    private Map<String, Object> series(String id, String name, String start, String end, int odi, int t20, int test) {
        return Map.of("id", id, "name", name, "startDate", start, "endDate", end,
                "odi", odi, "t20", t20, "test", test, "squads", 0, "matches", odi + t20 + test);
    }

    private List<Map<String, Object>> players() {
        return List.of(
                player("p1", "Virat Kohli", "Batter", "India", "Right-hand batter"),
                player("p2", "Jasprit Bumrah", "Bowler", "India", "Right-arm fast"),
                player("p3", "Pat Cummins", "Bowler", "Australia", "Right-arm fast"),
                player("p4", "Travis Head", "Batter", "Australia", "Left-hand batter"),
                player("p5", "Shakib Al Hasan", "All-rounder", "Bangladesh", "Left-hand batter | Slow left-arm orthodox"),
                player("p6", "Kane Williamson", "Batter", "New Zealand", "Right-hand batter"),
                player("p7", "Joe Root", "Batter", "England", "Right-hand batter"),
                player("p8", "Kagiso Rabada", "Bowler", "South Africa", "Right-arm fast"));
    }

    private Map<String, Object> player(String id, String name, String role, String country, String skills) {
        return Map.of("id", id, "name", name, "role", role, "image", "", "country", country,
                "matchesPlayed", "", "battingAverage", "", "bowlingAverage", "", "skills", skills);
    }
}

package com.cricory.backend.snapshot;

import com.cricory.backend.catalog.NewsScrapingService;
import com.cricory.backend.catalog.NewsDataResult;
import com.cricory.backend.scraping.CricinfoScrapingService;
import com.cricory.backend.scraping.MatchDataResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class ContentSnapshotSyncService {
    private static final Logger log = LoggerFactory.getLogger(ContentSnapshotSyncService.class);
    private final CricinfoScrapingService matchScraper;
    private final NewsScrapingService newsScraper;
    private final SnapshotStore store;
    private final SyncStatusService statusService;

    public ContentSnapshotSyncService(CricinfoScrapingService matchScraper,
                                      NewsScrapingService newsScraper,
                                      SnapshotStore store,
                                      SyncStatusService statusService) {
        this.matchScraper = matchScraper;
        this.newsScraper = newsScraper;
        this.store = store;
        this.statusService = statusService;
    }

    @Scheduled(initialDelayString = "20s", fixedDelayString = "${cricory.sync.live:20s}")
    public void syncLive() {
        syncMatch(SnapshotKeys.LIVE, matchScraper::liveMatchesResult);
    }

    @Scheduled(initialDelayString = "30s", fixedDelayString = "${cricory.sync.upcoming:30m}")
    public void syncUpcoming() {
        syncMatch(SnapshotKeys.UPCOMING, matchScraper::upcomingMatchesResult);
    }

    @Scheduled(initialDelayString = "40s", fixedDelayString = "${cricory.sync.recent:15m}")
    public void syncRecent() {
        syncMatch(SnapshotKeys.RECENT, matchScraper::recentMatchesResult);
    }

    @Scheduled(initialDelayString = "50s", fixedDelayString = "${cricory.sync.news:10m}")
    public void syncNews() {
        try {
            NewsDataResult result = newsScraper.latestNewsResult();
            persistNewsResult(result);
            if (result.remoteSuccess()) statusService.success(SnapshotKeys.NEWS, result.items().size());
            else statusService.failure(SnapshotKeys.NEWS, new IllegalStateException(result.source()));
        } catch (RuntimeException exception) {
            recordFailure(SnapshotKeys.NEWS, exception);
        }
    }

    @Scheduled(initialDelayString = "70s", fixedDelayString = "${cricory.sync.live-scorecards:60s}")
    public void syncLiveScorecards() {
        snapshotStoreMatches(SnapshotKeys.LIVE).stream().limit(5).forEach(this::syncScorecard);
    }

    @Scheduled(initialDelayString = "90s", fixedDelayString = "${cricory.sync.recent-scorecards:30m}")
    public void syncRecentScorecards() {
        snapshotStoreMatches(SnapshotKeys.RECENT).stream().limit(10).forEach(this::syncScorecard);
    }

    private void syncMatch(String key, java.util.function.Supplier<MatchDataResult> loader) {
        try {
            MatchDataResult result = loader.get();
            persistMatchResult(key, result);
            if (result.remoteSuccess()) statusService.success(key, result.items().size());
            else statusService.failure(key, new IllegalStateException(result.source()));
        } catch (RuntimeException exception) {
            recordFailure(key, exception);
        }
    }

    private void recordFailure(String key, RuntimeException exception) {
        statusService.failure(key, exception);
        log.warn("{} sync failed; last successful database snapshot was preserved", key, exception);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> snapshotStoreMatches(String key) {
        List<Map<String, Object>> stored = store.list(key);
        if (SnapshotKeys.LIVE.equals(key)) return stored;
        return stored.stream()
                .flatMap(group -> group.get("matches") instanceof List<?> list ? list.stream() : java.util.stream.Stream.empty())
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private void syncScorecard(Map<String, Object> match) {
        String id = String.valueOf(match.getOrDefault("match_id", ""));
        String link = String.valueOf(match.getOrDefault("detail_link", ""));
        if (id.isBlank() || link.isBlank() || !link.startsWith("/")) return;
        String key = SnapshotKeys.scorecard(id);
        try {
            Map<String, Object> scorecard = matchScraper.scorecard(id, link);
            store.save(key, scorecard, "CRICINFO_REMOTE");
            statusService.success(key, ((List<?>) scorecard.getOrDefault("innings", List.of())).size());
        } catch (RuntimeException exception) {
            recordFailure(key, exception);
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

}

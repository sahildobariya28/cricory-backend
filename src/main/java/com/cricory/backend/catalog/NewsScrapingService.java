package com.cricory.backend.catalog;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsScrapingService {
    private static final Logger log = LoggerFactory.getLogger(NewsScrapingService.class);
    private static final String ORIGIN = "https://www.cricinfo.com";
    private static final String IMAGE_CDN = "https://img1.hscicdn.com";
    private static final String NEWS_URL = ORIGIN + "/cricket-news";
    private static final String STORIES_POINTER = "/props/appPageProps/data/data/content/stories/results";

    private final ObjectMapper objectMapper;
    private final boolean remoteEnabled;
    private volatile CachedNews cache;

    public NewsScrapingService(ObjectMapper objectMapper,
                               @Value("${cricory.news.remote-enabled:true}") boolean remoteEnabled) {
        this.objectMapper = objectMapper;
        this.remoteEnabled = remoteEnabled;
    }

    public List<Map<String, Object>> latestNews() {
        return latestNewsResult().items();
    }

    public NewsDataResult latestNewsResult() {
        CachedNews current = cache;
        if (current != null && current.expiresAt().isAfter(Instant.now())) return current.result();
        synchronized (this) {
            current = cache;
            if (current != null && current.expiresAt().isAfter(Instant.now())) return current.result();
            NewsDataResult result = remoteEnabled ? scrapeSafely()
                    : unavailable();
            Duration ttl = result.remoteSuccess() ? Duration.ofMinutes(10) : Duration.ofMinutes(1);
            cache = new CachedNews(result, Instant.now().plus(ttl));
            return cache.result();
        }
    }

    private NewsDataResult scrapeSafely() {
        try {
            List<Map<String, Object>> stories = scrape();
            return stories.isEmpty()
                    ? unavailable()
                    : new NewsDataResult(List.copyOf(stories), true, "CRICINFO_REMOTE");
        } catch (RuntimeException exception) {
            log.warn("News scrape failed: {}", exception.getMessage(), exception);
            return unavailable();
        }
    }

    private NewsDataResult unavailable() {
        return new NewsDataResult(List.of(), false, "SCRAPE_UNAVAILABLE");
    }

    private List<Map<String, Object>> scrape() {
        String nextData;
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(true));
             BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                     .setLocale("en-IN")
                     .setTimezoneId("Asia/Kolkata")
                     .setViewportSize(1365, 768)
                     .setExtraHTTPHeaders(Map.of(
                             "Accept-Language", "en-IN,en-GB;q=0.9,en;q=0.8",
                             "Referer", ORIGIN + "/"))
                     .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                             + "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"))) {
            Page page = context.newPage();
            page.setDefaultTimeout(5_000);
            page.navigate(NEWS_URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(10_000));
            if (!page.url().startsWith(ORIGIN + "/")) throw new IllegalStateException("Unexpected news redirect");
            nextData = page.locator("#__NEXT_DATA__").textContent();
        }

        try {
            JsonNode stories = objectMapper.readTree(nextData).at(STORIES_POINTER);
            if (!stories.isArray()) throw new IllegalStateException("News stories were not found");
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode story : stories) result.add(mapStory(story));
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Cricinfo news", exception);
        }
    }

    private Map<String, Object> mapStory(JsonNode story) {
        JsonNode image = story.path("image");
        String imagePath = image.path("peerUrls").path("WIDE").asText("");
        if (imagePath.isBlank()) imagePath = image.path("url").asText("");
        String slug = story.path("slug").asText("");
        String objectId = story.path("objectId").asText(story.path("id").asText(""));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", objectId);
        item.put("title", story.path("title").asText(""));
        item.put("date", story.path("publishedAt").asText(""));
        item.put("img", absolute(imagePath));
        item.put("description", story.path("summary").asText(""));
        item.put("tag", story.path("genreName").asText("News"));
        item.put("link", ORIGIN + "/story/" + slug + "-" + objectId);
        return item;
    }

    private String absolute(String path) {
        if (path == null || path.isBlank()) return "";
        if (path.startsWith("http")) return path;
        if (path.startsWith("/image/upload/")) return IMAGE_CDN + path;
        if (path.startsWith("/lsci/")) {
            return IMAGE_CDN + "/image/upload/f_auto,t_ds_w_640" + path;
        }
        return ORIGIN + path;
    }

    private record CachedNews(NewsDataResult result, Instant expiresAt) { }
}

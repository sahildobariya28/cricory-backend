package com.cricory.backend.catalog;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
                    : new NewsDataResult(inspectedFallback(), false, "FALLBACK");
            cache = new CachedNews(result, Instant.now().plus(Duration.ofMinutes(10)));
            return cache.result();
        }
    }

    private NewsDataResult scrapeSafely() {
        try {
            List<Map<String, Object>> stories = scrape();
            return stories.isEmpty()
                    ? new NewsDataResult(inspectedFallback(), false, "FALLBACK")
                    : new NewsDataResult(List.copyOf(stories), true, "CRICINFO_REMOTE");
        } catch (RuntimeException ignored) {
            return new NewsDataResult(inspectedFallback(), false, "FALLBACK");
        }
    }

    private List<Map<String, Object>> scrape() {
        String nextData;
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                     .setChannel("chrome")
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

    private List<Map<String, Object>> inspectedFallback() {
        return List.of(
                fallback("1550025", "Forde's all-round starrer takes Kings past Falcons in rain-reduced thriller",
                        "Falcons managed just 98 in 19 overs before Kings took control", "2026-08-15T04:54:00.000Z",
                        "Report", "/lsci/db/PICTURES/CMS/421100/421191.6.jpg"),
                fallback("1550020", "Padikkal comes in as India opt to bat in Galle; Nuwantha debuts for Sri Lanka",
                        "Niroshan Dickwella has made a Test comeback while India picked three frontline spinners", "2026-08-15T04:20:00.000Z",
                        "News", "/lsci/db/PICTURES/CMS/421100/421138.6.jpg"),
                fallback("1550016", "Hazlewood becomes ninth Australian to take 300 Test wickets",
                        "Only three Australia bowlers have taken more Test wickets at a better average", "2026-08-15T03:54:00.000Z",
                        "News", "/lsci/db/PICTURES/CMS/421100/421187.6.jpg"),
                fallback("1550002", "Hazlewood gets six but not before Bangladesh take 228-run lead",
                        "Hazlewood picked up all four Bangladesh wickets to fall on the third day", "2026-08-15T02:51:00.000Z",
                        "Report", "/lsci/db/PICTURES/CMS/421100/421179.6.jpg"),
                fallback("1549998", "Cricket Australia chief highlights bilateral ODI revenue conundrum",
                        "CA says bilateral ODIs need more meaning while retaining their financial value", "2026-08-15T01:44:00.000Z",
                        "News", "/lsci/db/PICTURES/CMS/371700/371713.6.jpg"));
    }

    private Map<String, Object> fallback(String id, String title, String description,
                                         String date, String tag, String image) {
        return Map.of("id", id, "title", title, "description", description, "date", date,
                "tag", tag, "img", absolute(image), "link", ORIGIN + "/cricket-news");
    }

    private record CachedNews(NewsDataResult result, Instant expiresAt) { }
}

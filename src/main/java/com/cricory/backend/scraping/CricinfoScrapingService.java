package com.cricory.backend.scraping;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(prefix = "cricory.scraping", name = "enabled", havingValue = "true")
public class CricinfoScrapingService {
    private static final Logger log = LoggerFactory.getLogger(CricinfoScrapingService.class);

    private static final String ORIGIN = "https://www.cricinfo.com";
    private static final String IMAGE_CDN = "https://img1.hscicdn.com";
    private static final String LIVE_URL = ORIGIN + "/live-cricket-score";
    private static final String UPCOMING_URL = ORIGIN + "/live-cricket-match-schedule-fixtures";
    private static final String RESULTS_URL = ORIGIN + "/live-cricket-match-results";
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final ObjectMapper objectMapper;
    private final ScrapingProperties properties;
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();

    public CricinfoScrapingService(ObjectMapper objectMapper, ScrapingProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<Map<String, Object>> liveMatches() {
        return liveMatchesResult().items();
    }

    public MatchDataResult liveMatchesResult() {
        if (!properties.isRemoteEnabled()) return fallback();
        try { return remote(mapLive(loadMatches(LIVE_URL, false))); }
        catch (ScrapingException exception) { log.warn("Live scrape failed: {}", exception.getMessage(), exception); return fallback(); }
    }

    public List<Map<String, Object>> upcomingMatches() {
        return upcomingMatchesResult().items();
    }

    public MatchDataResult upcomingMatchesResult() {
        if (!properties.isRemoteEnabled()) return fallback();
        try { return remote(mapGrouped(loadMatches(UPCOMING_URL, true), MatchKind.UPCOMING)); }
        catch (ScrapingException exception) { log.warn("Upcoming scrape failed: {}", exception.getMessage(), exception); return fallback(); }
    }

    public List<Map<String, Object>> recentMatches() {
        return recentMatchesResult().items();
    }

    public MatchDataResult recentMatchesResult() {
        if (!properties.isRemoteEnabled()) return fallback();
        try { return remote(mapGrouped(loadMatches(RESULTS_URL, true), MatchKind.RESULT)); }
        catch (ScrapingException exception) { log.warn("Results scrape failed: {}", exception.getMessage(), exception); return fallback(); }
    }

    public Map<String, Object> scorecard(String matchId, String detailLink) {
        if (!properties.isRemoteEnabled()) throw new ScrapingException("Remote scraping is disabled", null);
        String scorecardPath = detailLink.replaceAll("/(live-cricket-score|full-scorecard)$", "/full-scorecard");
        JsonNode root = loadPageData(ORIGIN + scorecardPath);
        JsonNode match = root.at("/props/appPageProps/data/match");
        JsonNode innings = root.at("/props/appPageProps/data/content/innings");
        if (!innings.isArray()) throw new ScrapingException("Scorecard innings were not found", null);

        List<Map<String, Object>> mappedInnings = new ArrayList<>();
        for (JsonNode inning : innings) mappedInnings.add(mapInning(inning));
        return Map.of(
                "matchId", matchId,
                "status", text(match, "statusText", text(match, "status", "")),
                "innings", mappedInnings);
    }

    private MatchDataResult remote(List<Map<String, Object>> items) {
        return new MatchDataResult(List.copyOf(items), true, "CRICINFO_REMOTE");
    }

    private MatchDataResult fallback() {
        return new MatchDataResult(List.of(), false, "SCRAPE_UNAVAILABLE");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cached(String key, Duration ttl, Supplier<List<Map<String, Object>>> loader) {
        CachedValue current = cache.get(key);
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.value();
        }
        synchronized (cache) {
            current = cache.get(key);
            if (current != null && current.expiresAt().isAfter(Instant.now())) return current.value();
            List<Map<String, Object>> value = List.copyOf(loader.get());
            cache.put(key, new CachedValue(value, Instant.now().plus(ttl)));
            return value;
        }
    }

    private JsonNode loadMatches(String url, boolean nestedData) {
        JsonNode root = loadPageData(url);
        try {
            String pointer = nestedData
                    ? "/props/appPageProps/data/data/content/matches"
                    : "/props/appPageProps/data/content/matches";
            JsonNode matches = root.at(pointer);
            if (!matches.isArray()) throw new IllegalStateException("Cricinfo match data was not found");
            return matches;
        } catch (Exception exception) {
            throw new ScrapingException("Unable to parse Cricinfo structured data", exception);
        }
    }

    private JsonNode loadPageData(String url) {
        String nextData;
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                     new BrowserType.LaunchOptions()
                             .setHeadless(properties.isHeadless()));
            BrowserContext context = browser.newContext(
                     new Browser.NewContextOptions()
                             .setUserAgent(properties.getUserAgent())
                             .setLocale("en-IN")
                             .setTimezoneId("Asia/Kolkata")
                             .setViewportSize(1365, 768)
                             .setExtraHTTPHeaders(Map.of(
                                     "Accept-Language", "en-IN,en-GB;q=0.9,en;q=0.8",
                                     "Referer", ORIGIN + "/")))) {
            Page page = context.newPage();
            page.setDefaultTimeout(properties.getNavigationTimeout().toMillis());
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(properties.getNavigationTimeout().toMillis())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            if (!page.url().startsWith(ORIGIN + "/")) {
                throw new ScrapingException("Cricinfo redirected to an unexpected host", null);
            }
            if (page.locator("#__NEXT_DATA__").count() == 0) {
                String body = page.locator("body").innerText();
                String sample = body == null ? "" : body.substring(0, Math.min(body.length(), 500));
                throw new ScrapingException("Cricinfo page has no NEXT_DATA; url=" + page.url()
                        + ", title=" + page.title() + ", body=" + sample, null);
            }
            nextData = page.locator("#__NEXT_DATA__").textContent();
        } catch (RuntimeException exception) {
            throw new ScrapingException("Unable to load Cricinfo page", exception);
        }

        try {
            return objectMapper.readTree(nextData);
        } catch (Exception exception) {
            throw new ScrapingException("Unable to parse Cricinfo structured data", exception);
        }
    }

    private Map<String, Object> mapInning(JsonNode inning) {
        JsonNode team = inning.path("team");
        String title = text(team, "longName", text(team, "name", "Team")) + " Innings";
        String score = inning.path("runs").asText("0") + "/" + inning.path("wickets").asText("0")
                + " (" + inning.path("overs").asText("0") + " ov)";
        List<Map<String, Object>> batsmen = new ArrayList<>();
        for (JsonNode batter : inning.path("inningBatsmen")) {
            JsonNode player = batter.path("player");
            String dismissal = text(batter.path("dismissalText"), "long", batter.path("isOut").asBoolean() ? "out" : "not out");
            batsmen.add(Map.of(
                    "name", text(player, "longName", text(player, "name", "")),
                    "runs", batter.path("runs").asText("0"),
                    "balls", batter.path("balls").asText("0"),
                    "fours", batter.path("fours").asText("0"),
                    "sixes", batter.path("sixes").asText("0"),
                    "sr", batter.path("strikerate").asText("0"),
                    "dismissal", dismissal.trim(),
                    "isNotOut", !batter.path("isOut").asBoolean()));
        }
        List<Map<String, Object>> bowlers = new ArrayList<>();
        for (JsonNode bowler : inning.path("inningBowlers")) {
            JsonNode player = bowler.path("player");
            bowlers.add(Map.of(
                    "name", text(player, "longName", text(player, "name", "")),
                    "overs", bowler.path("overs").asText("0"),
                    "maidens", bowler.path("maidens").asText("0"),
                    "runs", bowler.path("conceded").asText("0"),
                    "wickets", bowler.path("wickets").asText("0"),
                    "eco", bowler.path("economy").asText("0")));
        }
        return Map.of("title", title, "scoreStr", score, "batsmen", batsmen, "bowlers", bowlers);
    }

    private List<Map<String, Object>> mapLive(JsonNode matches) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode match : matches) {
            try {
                if (!isLive(match)) continue;
                List<JsonNode> teams = validTeams(match);
                JsonNode teamOne = teams.get(0);
                JsonNode teamTwo = teams.get(1);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("match_id", requiredMatchId(match));
                item.put("detail_link", detailLink(match, "live-cricket-score"));
                item.put("title", matchTitle(match));
                item.put("status", text(match, "status", "Live"));
                item.put("team_1_flag", imageUrl(teamOne.path("team")));
                item.put("team_1", teamName(teamOne));
                item.put("team_1_score", scoreText(teamOne));
                item.put("team_2_flag", imageUrl(teamTwo.path("team")));
                item.put("team_2", teamName(teamTwo));
                item.put("team_2_score", scoreText(teamTwo));
                item.put("location", text(match.path("ground"), "smallName", ""));
                String startTime = text(match, "startTime", "");
                item.put("date", startTime.isBlank() ? "" : OffsetDateTime.parse(startTime)
                        .atZoneSameInstant(APP_ZONE).format(DATE));
                item.put("sub_status", text(match, "statusText", ""));
                item.put("crr", match.path("liveOvers").isNumber() ? String.valueOf(match.path("liveOvers").asDouble()) : "");
                item.put("venue", text(match.path("ground"), "longName", ""));
                item.put("match_summary", Map.of("text", text(match, "statusText", "")));
                item.put("teams", List.of(liveTeam(teamOne), liveTeam(teamTwo)));
                result.add(item);
            } catch (RuntimeException exception) {
                log.warn("Skipping invalid live match {}: {}", match.path("objectId").asText("unknown"), exception.getMessage());
            }
        }
        return result;
    }

    private List<Map<String, Object>> mapGrouped(JsonNode matches, MatchKind kind) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (JsonNode match : matches) {
            try {
                if (kind == MatchKind.UPCOMING && !isUpcoming(match)) continue;
                if (kind == MatchKind.RESULT && !isResult(match)) continue;
                OffsetDateTime start = OffsetDateTime.parse(text(match, "startTime", ""));
                String date = start.atZoneSameInstant(APP_ZONE).format(DATE);
                List<JsonNode> teams = validTeams(match);
                JsonNode teamOne = teams.get(0);
                JsonNode teamTwo = teams.get(1);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("match_id", requiredMatchId(match));
                item.put("match_name", matchTitle(match));
                item.put("detail_link", detailLink(match, kind == MatchKind.RESULT ? "full-scorecard" : "live-cricket-score"));
                item.put("time", start.atZoneSameInstant(APP_ZONE).format(DISPLAY_TIME));
                item.put("team_1", groupedTeam(teamOne));
                item.put("team_2", groupedTeam(teamTwo));
                item.put("location", text(match.path("ground"), "longName", ""));
                item.put("result", kind == MatchKind.RESULT ? text(match, "statusText", "") : "");
                groups.computeIfAbsent(date, ignored -> new ArrayList<>()).add(item);
            } catch (RuntimeException exception) {
                log.warn("Skipping invalid {} match {}: {}", kind, match.path("objectId").asText("unknown"), exception.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        groups.forEach((date, items) -> {
            Map<String, Object> dateInfo = Map.of(
                    "day", date,
                    "full_date", date,
                    "display_date", java.time.LocalDate.parse(date).format(DISPLAY_DATE));
            result.add(Map.of("date", dateInfo, "matches", items));
        });
        return result;
    }

    private Map<String, Object> liveTeam(JsonNode side) {
        JsonNode team = side.path("team");
        return Map.of(
                "name", text(team, "longName", text(team, "name", "")),
                "flag", imageUrl(team),
                // Keep Cricinfo's display score intact. Test matches can contain multiple
                // innings (for example "198 & 51/2 (17 ov)"), which cannot be represented
                // correctly by a single runs/wickets object.
                "score", scoreText(side));
    }

    private Map<String, Object> groupedTeam(JsonNode side) {
        JsonNode team = side.path("team");
        return Map.of(
                "name", text(team, "longName", text(team, "name", "")),
                "flag", imageUrl(team),
                "score", scoreText(side));
    }

    private Score parseScore(JsonNode side) {
        String raw = scoreText(side).replace("*", "").trim();
        String main = raw.split("\\s", 2)[0];
        int runs = 0;
        int wickets = 0;
        if (!main.isBlank()) {
            String[] parts = main.split("/", 2);
            runs = integer(parts[0]);
            wickets = parts.length > 1 ? integer(parts[1]) : 10;
        }
        String info = text(side, "scoreInfo", "");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*ov").matcher(info);
        double overs = matcher.find() ? Double.parseDouble(matcher.group(1)) : 0.0;
        return new Score(runs, wickets, overs);
    }

    private int integer(String value) {
        try { return Integer.parseInt(value.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private boolean isLive(JsonNode match) {
        return "LIVE".equalsIgnoreCase(match.path("state").asText()) || "RUNNING".equalsIgnoreCase(match.path("stage").asText());
    }

    private boolean isUpcoming(JsonNode match) {
        String state = match.path("state").asText();
        String stage = match.path("stage").asText();
        return "PRE".equalsIgnoreCase(state) || "SCHEDULED".equalsIgnoreCase(stage);
    }

    private boolean isResult(JsonNode match) {
        return "POST".equalsIgnoreCase(match.path("state").asText()) || "FINISHED".equalsIgnoreCase(match.path("stage").asText());
    }

    private String matchTitle(JsonNode match) {
        String title = text(match, "title", "");
        String series = text(match.path("series"), "name", "");
        return series.isBlank() ? title : title + ", " + series;
    }

    private String detailLink(JsonNode match, String page) {
        JsonNode series = match.path("series");
        return "/series/" + series.path("slug").asText() + "-" + series.path("objectId").asText()
                + "/" + match.path("slug").asText() + "-" + match.path("objectId").asText() + "/" + page;
    }

    private String teamName(JsonNode side) {
        JsonNode team = side.path("team");
        return text(team, "longName", text(team, "name", ""));
    }

    private String scoreText(JsonNode side) {
        String score = text(side, "score", "");
        String info = text(side, "scoreInfo", "");
        return info.isBlank() ? score : score + " (" + info + ")";
    }

    private String imageUrl(JsonNode team) {
        String path = text(team, "imageUrl", "");
        if (path.isBlank()) return path;
        String cricinfoPicturesPrefix = ORIGIN + "/lsci/";
        if (path.startsWith(cricinfoPicturesPrefix)) {
            return IMAGE_CDN + "/image/upload/f_auto,t_ds_square_w_160/lsci/"
                    + path.substring(cricinfoPicturesPrefix.length());
        }
        if (path.startsWith("http")) return path;
        if (path.startsWith("/image/upload/")) return IMAGE_CDN + path;
        if (path.startsWith("/lsci/")) {
            return IMAGE_CDN + "/image/upload/f_auto,t_ds_square_w_160" + path;
        }
        return ORIGIN + path;
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private List<JsonNode> elements(JsonNode array) {
        List<JsonNode> values = new ArrayList<>();
        if (array.isArray()) array.forEach(values::add);
        return values;
    }

    private List<JsonNode> validTeams(JsonNode match) {
        List<JsonNode> teams = elements(match.path("teams"));
        if (teams.size() < 2 || teamName(teams.get(0)).isBlank() || teamName(teams.get(1)).isBlank()) {
            throw new IllegalArgumentException("two valid teams are required");
        }
        return teams;
    }

    private int requiredMatchId(JsonNode match) {
        int id = match.path("objectId").asInt();
        if (id <= 0) throw new IllegalArgumentException("valid match id is required");
        return id;
    }

    private JsonNode at(List<JsonNode> values, int index) {
        return index < values.size() ? values.get(index) : objectMapper.createObjectNode();
    }

    private enum MatchKind { UPCOMING, RESULT }
    private record Score(int runs, int wickets, double overs) { }
    private record CachedValue(List<Map<String, Object>> value, Instant expiresAt) { }
}

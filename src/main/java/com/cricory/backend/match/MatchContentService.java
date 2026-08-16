package com.cricory.backend.match;

import com.cricory.backend.snapshot.SnapshotKeys;
import com.cricory.backend.snapshot.SnapshotStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cricory.backend.match.MatchApiModels.*;

@Service
public class MatchContentService {
    private final SnapshotStore snapshotStore;

    public MatchContentService(SnapshotStore snapshotStore) { this.snapshotStore = snapshotStore; }

    public MatchDetail detail(String id) {
        LocatedMatch located = allMatches().stream()
                .filter(item -> text(item.match(), "match_id").equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Match " + id + " nahi mila"));
        Map<String, Object> match = located.match();
        String name = text(match, "title");
        if (name.isBlank()) name = text(match, "match_name");
        String status = text(match, "status");
        if (status.isBlank()) status = text(match, "result");
        if (status.isBlank()) status = text(match, "time");
        String venue = text(match, "venue");
        if (venue.isBlank()) venue = text(match, "location");
        return new MatchDetail(id, name, typeOf(name), status, venue, located.date(), located.category(),
                text(match, "detail_link"), teams(match));
    }

    public Scorecard scorecard(String id) {
        MatchDetail detail = detail(id);
        Map<String, Object> stored = snapshotStore.map(SnapshotKeys.scorecard(id));
        return new Scorecard(id, text(stored, "status").isBlank() ? detail.status() : text(stored, "status"),
                innings(stored));
    }

    @SuppressWarnings("unchecked")
    private List<Inning> innings(Map<String, Object> scorecard) {
        if (!(scorecard.get("innings") instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(raw -> {
            Map<String, Object> inning = (Map<String, Object>) raw;
            return new Inning(text(inning, "title"), text(inning, "scoreStr"),
                    batsmen(inning), bowlers(inning));
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Batter> batsmen(Map<String, Object> inning) {
        if (!(inning.get("batsmen") instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(raw -> {
            Map<String, Object> batter = (Map<String, Object>) raw;
            return new Batter(text(batter, "name"), text(batter, "runs"), text(batter, "balls"),
                    text(batter, "fours"), text(batter, "sixes"), text(batter, "sr"),
                    text(batter, "dismissal"), Boolean.parseBoolean(text(batter, "isNotOut")));
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Bowler> bowlers(Map<String, Object> inning) {
        if (!(inning.get("bowlers") instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(raw -> {
            Map<String, Object> bowler = (Map<String, Object>) raw;
            return new Bowler(text(bowler, "name"), text(bowler, "overs"), text(bowler, "maidens"),
                    text(bowler, "runs"), text(bowler, "wickets"), text(bowler, "eco"));
        }).toList();
    }

    private List<LocatedMatch> allMatches() {
        List<LocatedMatch> result = new ArrayList<>();
        snapshotStore.list(SnapshotKeys.LIVE).forEach(match ->
                result.add(new LocatedMatch(match, "LIVE", text(match, "date"))));
        addGrouped(result, snapshotStore.list(SnapshotKeys.UPCOMING), "UPCOMING");
        addGrouped(result, snapshotStore.list(SnapshotKeys.RECENT), "RECENT");
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addGrouped(List<LocatedMatch> target, List<Map<String, Object>> groups, String category) {
        for (Map<String, Object> group : groups) {
            String date = "";
            if (group.get("date") instanceof Map<?, ?> dateMap) date = value(dateMap, "full_date");
            if (group.get("matches") instanceof List<?> matches) {
                for (Object match : matches) target.add(new LocatedMatch((Map<String, Object>) match, category, date));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Team> teams(Map<String, Object> match) {
        if (match.get("teams") instanceof List<?> list) {
            return list.stream().map(item -> teamFrom((Map<String, Object>) item)).toList();
        }
        List<Team> result = new ArrayList<>();
        if (match.get("team_1") instanceof Map<?, ?> first) result.add(teamFrom((Map<String, Object>) first));
        if (match.get("team_2") instanceof Map<?, ?> second) result.add(teamFrom((Map<String, Object>) second));
        return result;
    }

    private Team teamFrom(Map<String, Object> team) {
        Object rawScore = team.get("score");
        String score = rawScore instanceof Map<?, ?> values
                ? value(values, "runs") + "/" + value(values, "wickets") + " (" + value(values, "overs") + ")"
                : String.valueOf(rawScore == null ? "" : rawScore);
        return new Team(text(team, "name"), text(team, "flag"), score);
    }

    private String typeOf(String name) { return name.contains(",") ? name.substring(0, name.indexOf(',')).trim() : "Cricket"; }
    private String value(Map<?, ?> map, String key) { Object value = map.get(key); return value == null ? "" : String.valueOf(value); }
    private String text(Map<String, Object> map, String key) { return String.valueOf(map.getOrDefault(key, "")); }
    private record LocatedMatch(Map<String, Object> match, String category, String date) { }
}

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
        List<Team> teams = detail.teams();
        Team first = teams.isEmpty() ? new Team("Team 1", "", "") : teams.get(0);
        Team second = teams.size() < 2 ? new Team("Team 2", "", "") : teams.get(1);
        String firstScore = first.score().isBlank() ? "Score awaited" : first.score();
        String secondScore = second.score().isBlank() ? "Score awaited" : second.score();
        return new Scorecard(id, detail.status(), List.of(
                previewInning(first.name() + " Innings", firstScore, first.name(), second.name()),
                previewInning(second.name() + " Innings", secondScore, second.name(), first.name())
        ));
    }

    private Inning previewInning(String title, String score, String battingTeam, String bowlingTeam) {
        return new Inning(title, score,
                List.of(new Batter(battingTeam + " batting", "-", "-", "-", "-", "-",
                        "Detailed player data will appear after a successful live sync", true)),
                List.of(new Bowler(bowlingTeam + " bowling", "-", "-", "-", "-", "-")));
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

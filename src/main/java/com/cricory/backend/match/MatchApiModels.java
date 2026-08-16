package com.cricory.backend.match;

import java.util.List;

public final class MatchApiModels {
    private MatchApiModels() { }

    public record Team(String name, String imageUrl, String score) { }
    public record MatchDetail(String id, String name, String matchType, String status, String venue,
                              String date, String category, String detailLink, List<Team> teams) { }
    public record Scorecard(String matchId, String status, List<Inning> innings) { }
    public record Inning(String title, String scoreStr, List<Batter> batsmen, List<Bowler> bowlers) { }
    public record Batter(String name, String runs, String balls, String fours, String sixes,
                         String sr, String dismissal, boolean isNotOut) { }
    public record Bowler(String name, String overs, String maidens, String runs, String wickets, String eco) { }
}

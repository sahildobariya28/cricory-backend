package com.cricory.backend.scraping;

import java.util.List;
import java.util.Map;

/** Small inspected snapshot used only when Cricinfo blocks the local automated browser. */
final class PreviewMatchData {

    private PreviewMatchData() { }

    static List<Map<String, Object>> live() {
        return List.of(liveMatch(
                Map.entry("match_id", 1527273),
                Map.entry("detail_link", "/series/bangladesh-in-australia-2026-1527258/australia-vs-bangladesh-1st-test-1527273/live-cricket-score"),
                Map.entry("title", "1st Test, Bangladesh in Australia"),
                Map.entry("status", "TEA"),
                Map.entry("team_1_flag", "https://www.cricinfo.com/lsci/db/PICTURES/CMS/340400/340493.png"),
                Map.entry("team_1", "Australia"),
                Map.entry("team_1_score", "198 & 51/2 (17 ov)"),
                Map.entry("team_2_flag", "https://www.cricinfo.com/lsci/db/PICTURES/CMS/341400/341456.png"),
                Map.entry("team_2", "Bangladesh"),
                Map.entry("team_2_score", "426"),
                Map.entry("location", "Darwin"),
                Map.entry("sub_status", "Day 3 - Session 2: Australia trail by 177 runs."),
                Map.entry("crr", ""),
                Map.entry("venue", "Marrara Stadium, Darwin"),
                Map.entry("match_summary", Map.of("text", "Day 3 - Session 2: Australia trail by 177 runs.")),
                Map.entry("teams", List.of(
                        Map.of("name", "Australia", "flag", "https://www.cricinfo.com/lsci/db/PICTURES/CMS/340400/340493.png", "score", "198 & 51/2 (17 ov)"),
                        Map.of("name", "Bangladesh", "flag", "https://www.cricinfo.com/lsci/db/PICTURES/CMS/341400/341456.png", "score", "426")))),
                simpleLive(1544001, "1st Test, India tour of Sri Lanka 2026", "India", "Sri Lanka", "39/0 (9.5 ov)", "", "Galle", "LIVE", "Day 1 - Session 1: India chose to bat."),
                simpleLive(1527275, "3rd T20I, Pakistan in West Indies", "West Indies", "Pakistan", "154/6 (18.1 ov)", "153/8", "Bridgetown", "LIVE", "Preview snapshot"),
                simpleLive(1527276, "County Championship Division One", "Surrey", "Yorkshire", "312/5", "278", "London", "LIVE", "Preview snapshot"));
    }

    static List<Map<String, Object>> upcoming() {
        return grouped("2026-08-15", "15 Aug 2026", List.of(Map.ofEntries(
                Map.entry("match_id", 1544001),
                Map.entry("match_name", "1st Test, India in Sri Lanka"),
                Map.entry("detail_link", "/series/india-in-sri-lanka-2026-1543999/sri-lanka-vs-india-1st-test-1544001/live-cricket-score"),
                Map.entry("time", "10:00 AM"),
                Map.entry("team_1", team("Sri Lanka", "", "")),
                Map.entry("team_2", team("India", "", "")),
                Map.entry("location", "Galle"),
                Map.entry("result", "")),
                groupedMatch(1544002, "2nd ODI, New Zealand in England", "England", "New Zealand", "03:30 PM", "Manchester", ""),
                groupedMatch(1544003, "1st T20I, South Africa in Zimbabwe", "Zimbabwe", "South Africa", "05:00 PM", "Harare", ""),
                groupedMatch(1544004, "League Match, Caribbean Premier League", "Barbados Royals", "Trinbago Knight Riders", "07:30 PM", "Bridgetown", "")));
    }

    static List<Map<String, Object>> results() {
        return grouped("2026-08-14", "14 Aug 2026", List.of(Map.ofEntries(
                Map.entry("match_id", 1521263),
                Map.entry("match_name", "Eliminator, The Hundred Men's Competition"),
                Map.entry("detail_link", "/series/the-hundred-men-s-competition-2026-1521176/manchester-super-giants-men-vs-sunrisers-leeds-men-eliminator-1521263/full-scorecard"),
                Map.entry("time", "10:30 PM"),
                Map.entry("team_1", team("Manchester Super Giants (Men)", "", "186/4")),
                Map.entry("team_2", team("Sunrisers Leeds (Men)", "", "166/7 (100 balls, T:187)")),
                Map.entry("location", "Kennington Oval, London"),
                Map.entry("result", "Super Giants won by 20 runs")),
                groupedMatch(1521264, "3rd ODI, Australia in Bangladesh", "Bangladesh", "Australia", "02:00 PM", "Dhaka", "Australia won by 4 wickets"),
                groupedMatch(1521265, "Final, Women's T20 League", "Mumbai Women", "Delhi Women", "07:30 PM", "Mumbai", "Mumbai won by 7 runs"),
                groupedMatch(1521266, "2nd Test, England in South Africa", "South Africa", "England", "01:30 PM", "Cape Town", "Match drawn")));
    }

    private static Map<String, Object> team(String name, String flag, String score) {
        return Map.of("name", name, "flag", flag, "score", score);
    }

    @SafeVarargs
    private static Map<String, Object> liveMatch(Map.Entry<String, Object>... entries) {
        return Map.ofEntries(entries);
    }

    private static Map<String, Object> simpleLive(int id, String title, String team1, String team2,
                                                   String score1, String score2, String venue,
                                                   String status, String summary) {
        return Map.ofEntries(
                Map.entry("match_id", id), Map.entry("detail_link", "/match/" + id),
                Map.entry("title", title), Map.entry("status", status),
                Map.entry("team_1_flag", ""), Map.entry("team_1", team1), Map.entry("team_1_score", score1),
                Map.entry("team_2_flag", ""), Map.entry("team_2", team2), Map.entry("team_2_score", score2),
                Map.entry("location", venue), Map.entry("sub_status", summary), Map.entry("crr", ""),
                Map.entry("venue", venue), Map.entry("match_summary", Map.of("text", summary)),
                Map.entry("teams", List.of(team(team1, "", score1), team(team2, "", score2))));
    }

    private static Map<String, Object> groupedMatch(int id, String name, String team1, String team2,
                                                     String time, String location, String result) {
        return Map.ofEntries(Map.entry("match_id", id), Map.entry("match_name", name),
                Map.entry("detail_link", "/match/" + id), Map.entry("time", time),
                Map.entry("team_1", team(team1, "", "")), Map.entry("team_2", team(team2, "", "")),
                Map.entry("location", location), Map.entry("result", result));
    }

    private static List<Map<String, Object>> grouped(String date, String display, List<Map<String, Object>> matches) {
        return List.of(Map.of(
                "date", Map.of("day", date, "full_date", date, "display_date", display),
                "matches", matches));
    }
}

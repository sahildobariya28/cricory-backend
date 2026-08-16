# Cricory backend API contract

This contract was derived from the Android repositories, domain models and UI parsers.

## Data ownership

### Cricinfo-derived read-only cricket data

| Android call | Backend endpoint | Source | Refresh target |
|---|---|---|---|
| `getLiveMatches()` | `GET /api/live-matches` | `/live-cricket-score` | 20 seconds |
| `getUpcomingMatches()` | `GET /api/upcoming-matches` | `/live-cricket-match-schedule-fixtures` | 30 minutes |
| `getRecentMatches()` | `GET /api/recent-matches` | `/live-cricket-match-results` | 15 minutes |
| `getNews()` | `GET /api/news` | home/news structured data | 30 minutes |
| `getMatchTypes()` | `GET /api/get-match-types` | formats found in matches | 6 hours |
| `getMatchesByType(type)` | `GET /api/matches/{type}` | cached match lists | same as source list |
| `getSeriesList()` | `GET /api/series-list` | match series metadata | 6 hours |
| `searchSeries(query)` | `GET /api/series-search?search=` | local series index | 6 hours |
| `getMatchInfo(id)` / scorecard | `POST /api/match-info?id=` | derived full-scorecard URL | live: 20 seconds; result: permanent |
| `getSquad(id)` / all match detail | `GET /api/upcoming-matches/{id}` and `GET /api/all-matches/{id}` | `content.matchPlayers.teamPlayers` | 6 hours |
| `getLiveMatchDetail(id)` | `GET /api/live-matches/{id}` | match `content.innings` and players | 20 seconds |
| `getRecentMatchDetail(id)` | `GET /api/recent-matches/{id}` | match `content.innings` | permanent after result |
| `getPlayersList()` | `GET /api/players-list` | squads/player pages | 24 hours |
| `searchPlayer(query)` | `GET /api/player-search?search=` | local player index | 24 hours |
| `getPlayerInfo(id)` | `POST /api/player-info?id=` | player profile page | 24 hours |
| `getSeriesDetail(id)` | `POST /api/series-info?id=` | series page plus cached matches | 6 hours |
| fantasy points | `POST /api/fantacy-bbb?id=` | calculated by Cricory rules | live: 20 seconds |

The first three endpoints are implemented. The remaining rows define the next implementation phases.

### Cricory-owned data (never scraped)

- Authentication, OTP, profile and password management
- Wallet balance and immutable transactions
- Contests, prize breakup and contest entries
- User-created fantasy teams, captain and vice-captain
- Subscriptions and notification preferences

These require PostgreSQL/Firebase migration and normal authenticated CRUD APIs.

## Implemented response shapes

### `GET /api/live-matches`

```json
{
  "data": [
    {
      "match_id": 1527273,
      "detail_link": "/series/.../live-cricket-score",
      "title": "1st Test, Bangladesh in Australia",
      "status": "Live",
      "team_1": "Australia",
      "team_1_flag": "https://www.cricinfo.com/...png",
      "team_1_score": "198",
      "team_2": "Bangladesh",
      "team_2_flag": "https://www.cricinfo.com/...png",
      "team_2_score": "419/9 (135.5 ov)",
      "location": "Darwin",
      "venue": "Marrara Stadium, Darwin",
      "sub_status": "Day 3 - Session 2: Bangladesh lead...",
      "teams": [
        {
          "name": "Australia",
          "flag": "https://www.cricinfo.com/...png",
          "score": { "runs": 198, "wickets": 10, "overs": 0.0 }
        }
      ]
    }
  ]
}
```

### `GET /api/upcoming-matches`

```json
[
  {
    "date": {
      "day": "2026-08-15",
      "full_date": "2026-08-15",
      "display_date": "15 Aug 2026"
    },
    "matches": [
      {
        "match_id": 1544001,
        "match_name": "1st Test, India in Sri Lanka",
        "detail_link": "/series/.../live-cricket-score",
        "time": "10:00 AM",
        "team_1": { "name": "Sri Lanka", "flag": "https://...", "score": "" },
        "team_2": { "name": "India", "flag": "https://...", "score": "" },
        "location": "Galle International Stadium",
        "result": ""
      }
    ]
  }
]
```

`GET /api/recent-matches` uses the same grouped shape, with `result` populated and score values present.

## Structured source paths

The scraper reads the JSON script `#__NEXT_DATA__`; it does not depend on generated CSS classes.

- Live: `/props/appPageProps/data/content/matches`
- Fixtures/results: `/props/appPageProps/data/data/content/matches`
- Match metadata: `/props/appPageProps/data/match`
- Squads: `/props/appPageProps/data/content/matchPlayers/teamPlayers`
- Scorecard: `/props/appPageProps/data/content/innings`

The consent overlay does not block this embedded structured data, so repeatedly clicking the dialog is unnecessary.

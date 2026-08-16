package com.cricory.backend.scraping;

import java.util.List;
import java.util.Map;

public record MatchDataResult(List<Map<String, Object>> items, boolean remoteSuccess, String source) { }

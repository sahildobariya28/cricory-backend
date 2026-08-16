package com.cricory.backend.catalog;

import java.util.List;
import java.util.Map;

public record NewsDataResult(List<Map<String, Object>> items, boolean remoteSuccess, String source) { }

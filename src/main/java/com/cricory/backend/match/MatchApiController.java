package com.cricory.backend.match;

import com.cricory.backend.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cricory.backend.match.MatchApiModels.*;

@RestController
@RequestMapping("/api/matches")
public class MatchApiController {
    private final MatchContentService service;
    public MatchApiController(MatchContentService service) { this.service = service; }

    @GetMapping("/{id}")
    public ApiResponse<MatchDetail> detail(@PathVariable String id) {
        return ApiResponse.ok("Match detail loaded", service.detail(id));
    }

    @GetMapping("/{id}/scorecard")
    public ApiResponse<Scorecard> scorecard(@PathVariable String id) {
        return ApiResponse.ok("Scorecard loaded", service.scorecard(id));
    }
}

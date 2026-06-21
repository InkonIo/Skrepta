package com.skrepta.skreptajava.trending.controller;

import com.skrepta.skreptajava.trending.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/tags")
    public List<String> getTrendingTags(
        @RequestParam(defaultValue = "6") int limit
    ) {
        return trendingService.getTrendingTags(limit);
    }
}
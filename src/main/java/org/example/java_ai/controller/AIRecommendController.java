package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/ai/recommend")
@RequiredArgsConstructor
public class AIRecommendController {

    private final RecommendationService recommendationService;

    @GetMapping("/carousel")
    public ResponseEntity<Map<String, Object>> getCarouselRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "home") String scene,
            @RequestParam(defaultValue = "5") Integer limit) {

        log.info("获取AI推荐轮播 - userId: {}, scene: {}, limit: {}", userId, scene, limit);

        List<Map<String, Object>> recommendations;
        if (userId != null) {
            recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);
        } else {
            recommendations = recommendationService.getHotRecommendations(limit);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", recommendations, "scene", scene));
    }
}

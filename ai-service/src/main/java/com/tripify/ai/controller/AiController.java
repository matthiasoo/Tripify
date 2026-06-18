package com.tripify.ai.controller;

import com.tripify.ai.client.GroqClient;
import com.tripify.ai.dto.GeneratePlanRequest;
import com.tripify.ai.dto.PlaceRecommendation;
import com.tripify.ai.dto.RecommendPlacesRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final int DEFAULT_PLACES_COUNT = 6;

    private final GroqClient groqClient;

    public AiController(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    @PostMapping("/generate-plan")
    public ResponseEntity<String> generatePlan(@RequestBody GeneratePlanRequest request) {
        String plan = groqClient.generatePlan(
                request.city(),
                request.days(),
                request.pace(),
                request.temperature(),
                request.weatherDescription(),
                request.placesInfo()
        );
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/recommend-places")
    public ResponseEntity<List<PlaceRecommendation>> recommendPlaces(@RequestBody RecommendPlacesRequest request) {
        int count = request.count() != null && request.count() > 0 ? request.count() : DEFAULT_PLACES_COUNT;
        return ResponseEntity.ok(groqClient.recommendPlaces(request.city(), count));
    }
}

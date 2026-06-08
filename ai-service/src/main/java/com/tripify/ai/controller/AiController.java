package com.tripify.ai.controller;

import com.tripify.ai.client.GroqClient;
import com.tripify.ai.dto.GeneratePlanRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

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
}

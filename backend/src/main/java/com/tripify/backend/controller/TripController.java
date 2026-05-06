package com.tripify.backend.controller;

import com.tripify.backend.dto.TripPlanResponse;
import com.tripify.backend.service.TripPlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trip Planner", description = "Endpoints for planning trips using multiple data sources")
public class TripController {

    private final TripPlannerService tripPlannerService;

    public TripController(TripPlannerService tripPlannerService) {
        this.tripPlannerService = tripPlannerService;
    }

    @GetMapping("/plan/{city}")
    @Operation(summary = "Plan a trip", description = "Fetches weather and interesting places for a given city asynchronously.")
    public ResponseEntity<TripPlanResponse> planTrip(@PathVariable String city) {
        TripPlanResponse response = tripPlannerService.planTrip(city);
        return ResponseEntity.ok(response);
    }
}

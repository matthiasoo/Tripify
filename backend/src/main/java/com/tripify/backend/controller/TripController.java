package com.tripify.backend.controller;

import com.tripify.backend.dto.SaveTripPlanRequest;
import com.tripify.backend.dto.SavedTripPlanResponse;
import com.tripify.backend.dto.TripPlanResponse;
import com.tripify.backend.service.TripPlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Planer podróży", description = "Punkty końcowe do planowania podróży z wykorzystaniem wielu źródeł danych")
public class TripController {

    private final TripPlannerService tripPlannerService;

    public TripController(TripPlannerService tripPlannerService) {
        this.tripPlannerService = tripPlannerService;
    }

    @GetMapping("/plan/{city}")
    @Operation(summary = "Zaplanuj podróż", description = "Asynchronicznie pobiera pogodę i ciekawe miejsca dla podanego miasta.")
    public ResponseEntity<TripPlanResponse> planTrip(
            @PathVariable String city,
            @RequestParam(defaultValue = "1") int days,
            @RequestParam(defaultValue = "relaxed") String pace,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, userId(jwt));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    @Operation(summary = "Pobierz zapisane plany podróży", description = "Zwraca plany podróży utworzone przez bieżącego użytkownika.")
    public ResponseEntity<List<SavedTripPlanResponse>> getSavedPlans(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(tripPlannerService.getSavedPlans(userId(jwt)));
    }

    @PostMapping("/mine")
    @Operation(summary = "Zapisz wygenerowany plan podróży", description = "Zapisuje wygenerowany plan podróży dla bieżącego użytkownika.")
    public ResponseEntity<SavedTripPlanResponse> savePlan(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveTripPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripPlannerService.savePlan(userId(jwt), request));
    }

    @PostMapping("/mine/{planId}/regenerate")
    @Operation(summary = "Wygeneruj ponownie zapisany plan", description = "Ponownie uruchamia generowanie AI dla miasta zapisanego planu z wybraną długością i tempem, a następnie nadpisuje zapisany plan.")
    public ResponseEntity<SavedTripPlanResponse> regeneratePlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long planId,
            @RequestParam(defaultValue = "1") int days,
            @RequestParam(defaultValue = "relaxed") String pace
    ) {
        return ResponseEntity.ok(tripPlannerService.regeneratePlan(userId(jwt), planId, days, pace));
    }

    @DeleteMapping("/mine/{planId}")
    @Operation(summary = "Usuń zapisany plan podróży", description = "Usuwa zapisany plan podróży należący do bieżącego użytkownika.")
    public ResponseEntity<Void> deleteSavedPlan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long planId
    ) {
        tripPlannerService.deleteSavedPlan(userId(jwt), planId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Token nie zawiera identyfikatora użytkownika.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }
}

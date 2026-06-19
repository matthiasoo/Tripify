package com.tripify.backend.controller;

import com.tripify.backend.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
@Tag(name = "Pogoda", description = "Proxy dla API OpenWeather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    @Operation(summary = "Pobierz dane pogodowe",
               description = "Zwraca aktualną pogodę lub prognozę dla podanego miasta albo współrzędnych.")
    public ResponseEntity<?> getWeather(
            @RequestParam(defaultValue = "current") String type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String lat,
            @RequestParam(required = false) String lon,
            @RequestParam(defaultValue = "metric") String units,
            @RequestParam(defaultValue = "pl") String lang
    ) {
        try {
            Map<String, Object> data = weatherService.getWeather(type, city, lat, lon, units, lang);
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Nie udało się pobrać danych pogodowych."));
        }
    }
}

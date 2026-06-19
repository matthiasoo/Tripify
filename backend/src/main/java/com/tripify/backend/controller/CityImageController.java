package com.tripify.backend.controller;

import com.tripify.backend.dto.CityImageResponse;
import com.tripify.backend.service.CityImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/city-images")
@Tag(name = "Zdjęcia miast", description = "Proxy dla wyszukiwarki zdjęć Unsplash")
public class CityImageController {

    private final CityImageService cityImageService;

    public CityImageController(CityImageService cityImageService) {
        this.cityImageService = cityImageService;
    }

    @GetMapping
    @Operation(summary = "Wyszukaj zdjęcia miasta",
               description = "Zwraca stronicowane zdjęcia miasta z Unsplash.")
    public ResponseEntity<?> searchCityImages(
            @RequestParam String city,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "10") int perPage,
            @RequestParam(defaultValue = "landscape") String orientation
    ) {
        try {
            CityImageResponse response = cityImageService.search(city, page, perPage, orientation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", "Nie udało się pobrać zdjęć miasta."));
        }
    }
}

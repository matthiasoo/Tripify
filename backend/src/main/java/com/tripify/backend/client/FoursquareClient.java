package com.tripify.backend.client;

import com.tripify.backend.dto.PlaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FoursquareClient {
    private static final Logger log = LoggerFactory.getLogger(FoursquareClient.class);
    
    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;

    public FoursquareClient(RestClient restClient,
                            @Value("${api.foursquare.url}") String apiUrl,
                            @Value("${api.foursquare.key}") String apiKey) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public List<PlaceDto> getPlacesForCity(String city) {
        log.info("Fetching places for city: {} on virtual thread: {}", city, Thread.currentThread().isVirtual());
        
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("dummy-key")) {
            log.warn("Foursquare API key is not configured. Returning mock places.");
            return List.of(
                    new PlaceDto("Central Park", "Park", "New York, NY"),
                    new PlaceDto("Empire State Building", "Monument", "New York, NY")
            );
        }

        try {
            FoursquareSearchResponse response = restClient.get()
                    .uri(apiUrl + "/places/search?near={city}&limit=5", city)
                    .header("Authorization", apiKey)
                    .header("accept", "application/json")
                    .retrieve()
                    .body(FoursquareSearchResponse.class);

            if (response != null && response.results() != null) {
                return response.results().stream()
                        .map(result -> {
                            String category = (result.categories() != null && !result.categories().isEmpty())
                                    ? result.categories().get(0).name() : "Attraction";
                            String address = (result.location() != null)
                                    ? result.location().formatted_address() : "No address";
                            return new PlaceDto(result.name(), category, address);
                        })
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching places from Foursquare API", e);
            return List.of();
        }
    }

    // Foursquare API DTOs
    public record FoursquareSearchResponse(List<VenueResult> results) {}
    public record VenueResult(String name, List<Category> categories, LocationDetails location) {}
    public record Category(String name) {}
    public record LocationDetails(String formatted_address) {}
}

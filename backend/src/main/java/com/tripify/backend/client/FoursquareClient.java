package com.tripify.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripify.backend.dto.PlaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class FoursquareClient {
    private static final Logger log = LoggerFactory.getLogger(FoursquareClient.class);
    
    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;
    private final String apiVersion;

    public FoursquareClient(RestClient restClient,
                            @Value("${api.foursquare.url}") String apiUrl,
                            @Value("${api.foursquare.key}") String apiKey,
                            @Value("${api.foursquare.version}") String apiVersion) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiVersion = apiVersion;
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
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .path("/places/search")
                    .queryParam("near", city)
                    .queryParam("query", "atrakcje turystyczne")
                    .queryParam("limit", 5)
                    .queryParam("fields", "name,categories,location")
                    .build()
                    .encode()
                    .toUri();

            JsonNode response = restClient.get()
                    .uri(uri)
                    .header("Authorization", apiKey)
                    .header("X-Places-Api-Version", apiVersion)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), logFoursquareError())
                    .body(JsonNode.class);

            if (response != null && response.has("results") && response.get("results").isArray()) {
                List<PlaceDto> places = new ArrayList<>();
                response.get("results").forEach(result -> places.add(toPlaceDto(result)));
                return places;
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching places from Foursquare API: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private PlaceDto toPlaceDto(JsonNode result) {
        String name = textOrDefault(result, "name", "Unknown place");
        String category = extractCategory(result);
        String address = extractAddress(result);
        return new PlaceDto(name, category, address);
    }

    private String extractCategory(JsonNode result) {
        JsonNode categories = result.get("categories");
        if (categories != null && categories.isArray() && !categories.isEmpty()) {
            JsonNode first = categories.get(0);
            if (first.hasNonNull("name")) {
                return first.get("name").asText();
            }
            if (first.isTextual()) {
                return first.asText();
            }
        }
        return "Attraction";
    }

    private String extractAddress(JsonNode result) {
        JsonNode location = result.get("location");
        if (location != null) {
            if (location.hasNonNull("formatted_address")) {
                return location.get("formatted_address").asText();
            }
            if (location.hasNonNull("address")) {
                return location.get("address").asText();
            }

            List<String> parts = new ArrayList<>();
            addLocationPart(parts, location, "locality");
            addLocationPart(parts, location, "region");
            addLocationPart(parts, location, "country");
            if (!parts.isEmpty()) {
                return String.join(", ", parts);
            }
        }

        return "No address";
    }

    private void addLocationPart(List<String> parts, JsonNode location, String field) {
        if (location.hasNonNull(field)) {
            parts.add(location.get(field).asText());
        }
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        return node.hasNonNull(field) ? node.get(field).asText() : fallback;
    }

    private ErrorHandler logFoursquareError() {
        return (request, response) -> {
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            log.error(
                    "Foursquare API returned {} {} for {}. Body: {}",
                    response.getStatusCode().value(),
                    response.getStatusText(),
                    request.getURI(),
                    body
            );
            throw new IllegalStateException("Foursquare API error: " + response.getStatusCode().value());
        };
    }
}

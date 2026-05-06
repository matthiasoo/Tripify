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
        
        try {
            // Tymczasowo zwracamy zamockowane dane
            Thread.sleep(1500); // symulacja opóźnienia, by udowodnić równoległość
            return List.of(
                    new PlaceDto("Central Park", "Park", "New York, NY"),
                    new PlaceDto("Empire State Building", "Monument", "New York, NY")
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        } catch (Exception e) {
            log.error("Error fetching places", e);
            return List.of();
        }
    }
}

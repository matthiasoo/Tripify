package com.tripify.backend.client;

import com.tripify.backend.dto.UnsplashSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UnsplashClient {
    private static final Logger log = LoggerFactory.getLogger(UnsplashClient.class);
    private static final String BASE_URL = "https://api.unsplash.com";

    private final RestClient restClient;
    private final String accessKey;

    public UnsplashClient(RestClient restClient,
                          @Value("${api.unsplash.key}") String accessKey) {
        this.restClient = restClient;
        this.accessKey = accessKey;
    }

    public UnsplashSearchResponse searchPhotos(String city, int page, int perPage, String orientation) {
        log.info("Searching Unsplash for city: {}", city);

        try {
            return restClient.get()
                    .uri(BASE_URL + "/search/photos?query={query}&page={page}&per_page={perPage}" +
                                    "&orientation={orientation}&content_filter=high&order_by=relevant",
                            city + " city", page, perPage, orientation)
                    .header("Authorization", "Client-ID " + accessKey)
                    .header("Accept-Version", "v1")
                    .retrieve()
                    .body(UnsplashSearchResponse.class);
        } catch (Exception e) {
            log.error("Error searching Unsplash photos", e);
            throw new RuntimeException("Failed to fetch city images", e);
        }
    }
}

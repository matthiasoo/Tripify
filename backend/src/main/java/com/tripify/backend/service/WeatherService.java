package com.tripify.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;

    public WeatherService(RestClient restClient,
                          @Value("${api.openweather.url}") String apiUrl,
                          @Value("${api.openweather.key}") String apiKey) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * Proxy weather request to OpenWeather API, returning raw JSON as a Map
     * so the frontend receives the exact same shape it used to get.
     */
    public Map<String, Object> getWeather(String type, String city, String lat, String lon,
                                          String units, String lang) {
        String endpoint = "forecast".equals(type) ? "forecast" : "weather";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/" + endpoint)
                .queryParam("appid", apiKey)
                .queryParam("units", units)
                .queryParam("lang", lang);

        if (lat != null && lon != null) {
            builder.queryParam("lat", lat);
            builder.queryParam("lon", lon);
        } else if (city != null) {
            builder.queryParam("q", city);
        } else {
            throw new IllegalArgumentException("Provide either 'city' or both 'lat' and 'lon' parameters.");
        }

        String url = builder.build().toUriString();
        log.info("Fetching weather: {}", url.replaceAll("appid=[^&]+", "appid=***"));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

        return response;
    }
}

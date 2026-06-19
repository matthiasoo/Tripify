package com.tripify.backend.client;

import com.tripify.backend.dto.WeatherDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenWeatherClient {
    private static final Logger log = LoggerFactory.getLogger(OpenWeatherClient.class);

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;

    public OpenWeatherClient(RestClient restClient,
            @Value("${api.openweather.url}") String apiUrl,
            @Value("${api.openweather.key}") String apiKey) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public WeatherDto getWeatherForCity(String city) {
        log.info("Fetching weather for city: {} on virtual thread: {}", city, Thread.currentThread().isVirtual());

        try {
            com.tripify.backend.dto.OpenWeatherResponse response = restClient.get()
                    .uri(apiUrl + "/weather?q={city}&appid={key}&units=metric&lang=pl", city, apiKey)
                    .retrieve()
                    .body(com.tripify.backend.dto.OpenWeatherResponse.class);

            if (response != null && response.main() != null) {
                String desc = (response.weather() != null && !response.weather().isEmpty())
                        ? response.weather().getFirst().description()
                        : "No description";
                return new WeatherDto(response.main().temp(), desc);
            }
            return new WeatherDto(0.0, "Unknown");
        } catch (Exception e) {
            log.error("Error fetching weather from API", e);
            return new WeatherDto(0.0, "Unknown");
        }
    }
}

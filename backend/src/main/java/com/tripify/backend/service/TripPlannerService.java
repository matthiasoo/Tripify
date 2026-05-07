package com.tripify.backend.service;

import com.tripify.backend.client.FoursquareClient;
import com.tripify.backend.client.OpenWeatherClient;
import com.tripify.backend.dto.PlaceDto;
import com.tripify.backend.dto.TripPlanResponse;
import com.tripify.backend.dto.WeatherDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TripPlannerService {
    private static final Logger log = LoggerFactory.getLogger(TripPlannerService.class);

    private final OpenWeatherClient openWeatherClient;
    private final FoursquareClient foursquareClient;
    private final ExecutorService virtualThreadExecutor;

    public TripPlannerService(OpenWeatherClient openWeatherClient, FoursquareClient foursquareClient) {
        this.openWeatherClient = openWeatherClient;
        this.foursquareClient = foursquareClient;
        // Tworzymy Executor oparty na Virtual Threads z JDK 21
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public TripPlanResponse planTrip(String city) {
        log.info("Starting trip planning for {} on main thread. Is virtual: {}", city,
                Thread.currentThread().isVirtual());

        // Uruchamiamy pobieranie danych równolegle w wirtualnych wątkach
        CompletableFuture<WeatherDto> weatherFuture = CompletableFuture.supplyAsync(
                () -> openWeatherClient.getWeatherForCity(city), virtualThreadExecutor);

        CompletableFuture<List<PlaceDto>> placesFuture = CompletableFuture.supplyAsync(
                () -> foursquareClient.getPlacesForCity(city), virtualThreadExecutor);

        try {
            // Czekamy na zakończenie obu zadań (maksymalny czas oczekiwania to czas trwania
            // najdłuższego z nich)
            CompletableFuture.allOf(weatherFuture, placesFuture).join();

            WeatherDto weather = weatherFuture.get();
            List<PlaceDto> places = placesFuture.get();

            return new TripPlanResponse(city, weather, places);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error occurred while planning trip", e);
            throw new RuntimeException("Could not plan trip", e);
        }
    }
}

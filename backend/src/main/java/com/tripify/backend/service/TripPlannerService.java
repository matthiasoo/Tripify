package com.tripify.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tripify.backend.client.OpenWeatherClient;
import org.springframework.web.client.RestClient;
import com.tripify.backend.dto.PlaceDto;
import com.tripify.backend.dto.SaveTripPlanRequest;
import com.tripify.backend.dto.SavedTripPlanResponse;
import com.tripify.backend.dto.TripPlanResponse;
import com.tripify.backend.dto.WeatherDto;
import com.tripify.backend.model.SavedTripPlan;
import com.tripify.backend.repository.SavedTripPlanRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TripPlannerService {
    private static final Logger log = LoggerFactory.getLogger(TripPlannerService.class);

    private final OpenWeatherClient openWeatherClient;
    private final RestClient aiServiceClient;
    private final SavedTripPlanRepository savedTripPlanRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService virtualThreadExecutor;

    public TripPlannerService(OpenWeatherClient openWeatherClient, 
                              RestClient.Builder restClientBuilder,
                              SavedTripPlanRepository savedTripPlanRepository,
                              ObjectMapper objectMapper) {
        this.openWeatherClient = openWeatherClient;
        this.aiServiceClient = restClientBuilder.baseUrl("http://ai-service").build();
        this.savedTripPlanRepository = savedTripPlanRepository;
        this.objectMapper = objectMapper;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public TripPlanResponse planTrip(String city, int days, String pace, Long userId) {
        log.info("Starting trip planning for {} days (pace: {}) in {} on main thread. Is virtual: {}", days, pace, city,
                Thread.currentThread().isVirtual());

        CompletableFuture<WeatherDto> weatherFuture = CompletableFuture.supplyAsync(
                () -> openWeatherClient.getWeatherForCity(city), virtualThreadExecutor);

        try {
            weatherFuture.join();

            WeatherDto weather = weatherFuture.get();
            List<PlaceDto> places = List.of();

            List<String> placesInfo = List.of();

            Map<String, Object> requestBody = Map.of(
                    "city", city,
                    "days", days,
                    "pace", pace,
                    "temperature", weather.temperature(),
                    "weatherDescription", weather.description(),
                    "placesInfo", placesInfo
            );

            String plan = null;
            try {
                plan = aiServiceClient.post()
                        .uri("/api/v1/ai/generate-plan")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                log.warn("Could not reach ai-service", e);
            }

            if (plan == null || plan.isBlank()) {
                plan = generateLocalFallbackPlan(city, days, pace, weather.temperature(), weather.description(), places);
            }

            return new TripPlanResponse(city, weather, places, plan);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error occurred while planning trip", e);
            throw new RuntimeException("Could not plan trip", e);
        }
    }

    @Transactional
    public SavedTripPlanResponse savePlan(Long userId, SaveTripPlanRequest request) {
        List<PlaceDto> places = request.places() == null ? List.of() : request.places();
        SavedTripPlan savedTripPlan = savedTripPlanRepository.save(new SavedTripPlan(
                userId,
                request.city(),
                request.weather().temperature(),
                request.weather().description(),
                serializePlaces(places),
                request.plan()
        ));

        return toSavedTripPlanResponse(savedTripPlan);
    }

    @Transactional
    public void deleteSavedPlan(Long userId, Long planId) {
        if (!savedTripPlanRepository.existsByIdAndUserId(planId, userId)) {
            throw new IllegalArgumentException("Nie znaleziono zapisanego planu podróży.");
        }
        savedTripPlanRepository.deleteById(planId);
    }

    public List<SavedTripPlanResponse> getSavedPlans(Long userId) {
        return savedTripPlanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSavedTripPlanResponse)
                .toList();
    }

    private SavedTripPlanResponse toSavedTripPlanResponse(SavedTripPlan savedTripPlan) {
        return new SavedTripPlanResponse(
                savedTripPlan.getId(),
                savedTripPlan.getCity(),
                new WeatherDto(savedTripPlan.getWeatherTemperature(), savedTripPlan.getWeatherDescription()),
                deserializePlaces(savedTripPlan.getPlacesJson()),
                savedTripPlan.getPlan(),
                savedTripPlan.getCreatedAt()
        );
    }

    private String serializePlaces(List<PlaceDto> places) {
        try {
            return objectMapper.writeValueAsString(places);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize trip places. Saving an empty list.", e);
            return "[]";
        }
    }

    private List<PlaceDto> deserializePlaces(String placesJson) {
        try {
            return objectMapper.readValue(placesJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize saved trip places.", e);
            return List.of();
        }
    }

    private String generateLocalFallbackPlan(String city, int days, String pace, double temp, String weatherDesc, List<PlaceDto> places) {
        StringBuilder sb = new StringBuilder();
        boolean isIntense = "intense".equalsIgnoreCase(pace) || "intensywny".equalsIgnoreCase(pace);
        
        sb.append(String.format("# 📍 Spersonalizowany Plan Podróży: %s (%d dni, tempo: %s)\n\n", 
                city, days, isIntense ? "Intensywne" : "Luźne"));
        sb.append(String.format("### 🌤️ Aktualna Pogoda w Mieście\n- **Temperatura:** %.1f°C\n- **Warunki:** %s\n\n", temp, weatherDesc));

        sb.append("### 🧥 Sugestia Ubioru & Przygotowania\n");
        if (temp < 10) {
            sb.append("- Jest dość chłodno. Zalecamy ubranie się ciepło na cebulkę (ciepła kurtka, czapka, szalik). Warto wziąć ze sobą termos z ciepłym napojem.\n");
        } else if (temp >= 10 && temp < 20) {
            sb.append("- Temperatura jest umiarkowana. Lekka kurtka przejściowa, softshell lub cieplejszy sweter będą idealnym wyborem.\n");
        } else {
            sb.append("- Pogoda sprzyja lekkiemu ubiorowi! Pamiętaj o nakryciu głowy, okularach przeciwsłonecznych, filtrze UV oraz zabraniu dużej ilości wody.\n");
        }

        String lowerDesc = weatherDesc.toLowerCase();
        if (lowerDesc.contains("rain") || lowerDesc.contains("drizzle") || lowerDesc.contains("shower") || lowerDesc.contains("storm") || lowerDesc.contains("deszcz")) {
            sb.append("- **Uwaga:** Zapowiadane są opady deszczu. Koniecznie zabierz ze sobą parasol lub kurtkę przeciwdeszczową. Warto skupić się na atrakcjach wewnątrz budynków.\n");
        } else {
            sb.append("- Brak prognozowanego deszczu. Pogoda jest doskonała do spacerów na świeżym powietrzu i zwiedzania miejskich zakątków.\n");
        }
        sb.append("\n");

        for (int d = 1; d <= days; d++) {
            sb.append(String.format("## 📅 Dzień %d\n\n", d));
            
            // Podział atrakcji z Foursquare (jeśli są dostępne) na poszczególne dni
            int placeIndex = (d - 1) * (isIntense ? 2 : 1);
            
            sb.append("### 🌅 Rano\n");
            if (places != null && placeIndex < places.size()) {
                PlaceDto p = places.get(placeIndex);
                sb.append(String.format("- Wizyta w: **%s** (Kategoria: *%s*)\n  - *Adres:* %s\n", p.name(), p.category(), p.address()));
            } else {
                sb.append("- Spacer po zabytkowym centrum miasta i śniadanie w przytulnej kawiarni.\n");
            }
            if (isIntense) {
                sb.append("- Szybkie zwiedzanie pobliskiego punktu widokowego lub galerii sztuki.\n");
            }
            sb.append("\n");

            sb.append("### ☀️ Popołudnie\n");
            int secondPlaceIndex = placeIndex + 1;
            if (places != null && !isIntense && secondPlaceIndex < places.size()) {
                PlaceDto p = places.get(secondPlaceIndex);
                sb.append(String.format("- Wizyta w: **%s** (Kategoria: *%s*)\n  - *Adres:* %s\n", p.name(), p.category(), p.address()));
            } else if (places != null && isIntense && secondPlaceIndex < places.size()) {
                PlaceDto p = places.get(secondPlaceIndex);
                sb.append(String.format("- Wizyta w: **%s** (Kategoria: *%s*)\n  - *Adres:* %s\n", p.name(), p.category(), p.address()));
                sb.append("- Krótki odpoczynek na lunch w tradycyjnej restauracji.\n");
            } else {
                sb.append("- Tradycyjny lunch i relaks w parku miejskim lub spacer wzdłuż głównych ulic handlowych.\n");
            }
            sb.append("\n");

            sb.append("### 🌆 Wieczór\n");
            sb.append("- Kolacja w wysoko ocenianej restauracji serwującej lokalne specjały kulinarne.\n");
            if (isIntense) {
                sb.append("- Nocny spacer po rozświetlonym mieście lub wizyta w teatrze/lokalnym klubie z muzyką na żywo.\n");
            } else {
                sb.append("- Relaks przy filiżance herbaty lub kieliszku wina w spokojnej atmosferze.\n");
            }
            sb.append("\n");
        }

        sb.append("---\n*Wskazówka: Powyższy plan został skomponowany automatycznie jako awaryjny plan lokalny.*");
        return sb.toString();
    }
}

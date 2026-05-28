package com.tripify.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.backend.client.FoursquareClient;
import com.tripify.backend.client.OpenWeatherClient;
import com.tripify.backend.client.GeminiClient;
import com.tripify.backend.dto.PlaceDto;
import com.tripify.backend.dto.SaveTripPlanRequest;
import com.tripify.backend.dto.SavedTripPlanResponse;
import com.tripify.backend.dto.TripPlanResponse;
import com.tripify.backend.dto.WeatherDto;
import com.tripify.backend.model.AppUser;
import com.tripify.backend.model.SavedTripPlan;
import com.tripify.backend.repository.SavedTripPlanRepository;
import jakarta.transaction.Transactional;
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
    private final GeminiClient geminiClient;
    private final SavedTripPlanRepository savedTripPlanRepository;
    private final ObjectMapper objectMapper;
    private final ExecutorService virtualThreadExecutor;

    public TripPlannerService(OpenWeatherClient openWeatherClient, 
                              FoursquareClient foursquareClient,
                              GeminiClient geminiClient,
                              SavedTripPlanRepository savedTripPlanRepository,
                              ObjectMapper objectMapper) {
        this.openWeatherClient = openWeatherClient;
        this.foursquareClient = foursquareClient;
        this.geminiClient = geminiClient;
        this.savedTripPlanRepository = savedTripPlanRepository;
        this.objectMapper = objectMapper;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public TripPlanResponse planTrip(String city, AppUser user) {
        log.info("Starting trip planning for {} on main thread. Is virtual: {}", city,
                Thread.currentThread().isVirtual());

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

            List<String> placesInfo = places.stream()
                    .map(p -> String.format("- %s (%s) pod adresem %s", p.name(), p.category(), p.address()))
                    .toList();

            String plan = geminiClient.generatePlan(city, weather.temperature(), weather.description(), placesInfo);

            if (plan == null || plan.isBlank()) {
                plan = generateLocalFallbackPlan(city, weather.temperature(), weather.description(), places);
            }

            return new TripPlanResponse(city, weather, places, plan);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error occurred while planning trip", e);
            throw new RuntimeException("Could not plan trip", e);
        }
    }

    @Transactional
    public SavedTripPlanResponse savePlan(AppUser user, SaveTripPlanRequest request) {
        List<PlaceDto> places = request.places() == null ? List.of() : request.places();
        SavedTripPlan savedTripPlan = savedTripPlanRepository.save(new SavedTripPlan(
                user,
                request.city(),
                request.weather().temperature(),
                request.weather().description(),
                serializePlaces(places),
                request.plan()
        ));

        return toSavedTripPlanResponse(savedTripPlan);
    }

    @Transactional
    public void deleteSavedPlan(AppUser user, Long planId) {
        if (!savedTripPlanRepository.existsByIdAndUser(planId, user)) {
            throw new IllegalArgumentException("Nie znaleziono zapisanego planu podróży.");
        }
        savedTripPlanRepository.deleteById(planId);
    }

    public List<SavedTripPlanResponse> getSavedPlans(AppUser user) {
        return savedTripPlanRepository.findByUserOrderByCreatedAtDesc(user).stream()
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

    private String generateLocalFallbackPlan(String city, double temp, String weatherDesc, List<PlaceDto> places) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# 📍 Spersonalizowany Plan Podróży: %s\n\n", city));
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

        sb.append("### 🗺️ Propozycja Harmonogramu Dnia\n\n");
        
        if (places == null || places.isEmpty()) {
            sb.append("#### 🌅 Rano (09:00 - 12:00)\n- Rozpocznij dzień od wizyty w przytulnej lokalnej kawiarni w ścisłym centrum miasta. Zamów tradycyjne śniadanie i zaplanuj spacer.\n");
            sb.append("\n#### ☀️ Popołudnie (13:00 - 17:00)\n- Udaj się na relaksujący spacer wokół historycznego rynku lub głównego deptaka miejskiego. Spędź czas na robieniu zdjęć i podziwianiu architektury.\n");
            sb.append("\n#### 🌆 Wieczór (18:00 - 21:00)\n- Zjedz pyszną kolację w wysoko ocenianej restauracji serwującej lokalne specjały kulinarne. Spróbuj tradycyjnych dań i zrelaksuj się po całym dniu.\n");
        } else {
            int count = places.size();
            PlaceDto first = places.get(0);
            PlaceDto second = count > 1 ? places.get(1) : null;
            PlaceDto third = count > 2 ? places.get(2) : null;

            sb.append(String.format("#### 🌅 Rano (09:00 - 12:00)\n- **Wizyta w: %s** (Kategoria: *%s*)\n  - *Adres:* %s\n", first.name(), first.category(), first.address()));
            if (lowerDesc.contains("rain") || lowerDesc.contains("deszcz")) {
                sb.append("  - *Wskazówka:* Z uwagi na deszcz, zacznij od tej lokalizacji. Jeśli to otwarta przestrzeń, upewnij się, że masz parasol.\n");
            } else {
                sb.append("  - *Wskazówka:* Doskonałe miejsce na rozpoczęcie dnia, kiedy rano jest najmniej turystów.\n");
            }

            if (second != null) {
                sb.append(String.format("\n#### ☀️ Popołudnie (13:00 - 17:00)\n- **Wizyta w: %s** (Kategoria: *%s*)\n  - *Adres:* %s\n", second.name(), second.category(), second.address()));
                if (lowerDesc.contains("rain") || lowerDesc.contains("deszcz")) {
                    sb.append("  - *Wskazówka:* Po zwiedzaniu, zrób przerwę na obiad w zadaszonej restauracji w bliskiej odległości.\n");
                } else {
                    sb.append("  - *Wskazówka:* Poświęć ten czas na spacer w okolicy i skosztowanie lokalnego street foodu.\n");
                }
            } else {
                sb.append("\n#### ☀️ Popołudnie (13:00 - 17:00)\n- Czas na smaczny lunch w klimatycznej knajpce w centrum i dalsze odkrywanie sekretnych zaułków miasta.\n");
            }

            if (third != null) {
                sb.append(String.format("\n#### 🌆 Wieczór (18:00 - 21:00)\n- **Wizyta w: %s** (Kategoria: *%s*)\n  - *Adres:* %s\n", third.name(), third.category(), third.address()));
                sb.append("  - *Wskazówka:* Wspaniałe zwieńczenie dnia. Podziwiaj miasto w wieczornych barwach i zrób pamiątkowe zdjęcia przed powrotem do hotelu.\n");
            } else {
                sb.append("\n#### 🌆 Wieczór (18:00 - 21:00)\n- Udaj się do lokalnego baru lub kawiarni z muzyką na żywo, zrelaksuj się przy napoju i podsumuj dzień pełen wrażeń.\n");
            }
        }

        sb.append("\n---\n*Wskazówka: Powyższy plan został skomponowany automatycznie na podstawie aktualnych odczytów pogodowych i najpopularniejszych atrakcji Foursquare dla Twojego bezpieczeństwa i wygody.*");
        return sb.toString();
    }
}

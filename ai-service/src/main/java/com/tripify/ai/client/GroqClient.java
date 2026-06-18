package com.tripify.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.ai.dto.PlaceRecommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GroqClient {
    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public GroqClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${api.groq.url}") String apiUrl,
            @Value("${api.groq.key}") String apiKey,
            @Value("${api.groq.model}") String model
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    private boolean isApiKeyMissing() {
        return apiKey == null || apiKey.isBlank() || apiKey.equals("dummy-key");
    }

    public String generatePlan(String city, int days, String pace, double temp, String weatherDesc, List<String> placesInfo) {
        if (isApiKeyMissing()) {
            log.warn("Groq API key is not configured. Falling back to local rules-based planner.");
            return null;
        }

        try {
            String translatedPace = "relaxed".equalsIgnoreCase(pace) || "luzny".equalsIgnoreCase(pace) ? "luźne/spokojne" : "intensywne/aktywne";
            String prompt = String.format(
                    "Jesteś profesjonalnym agentem AI wspierającym planowanie podróży. " +
                            "Stwórz spersonalizowany, szczegółowy plan podróży dla miasta: %s na okres %d dni.\n" +
                            "Preferowane tempo wyjazdu: %s.\n" +
                            "Aktualna pogoda: %.1f°C, %s.\n" +
                            "Jeśli podano poniższe atrakcje, możesz je uwzględnić w planie. Jeśli lista jest pusta lub niepełna, samodzielnie dobierz najciekawsze atrakcje i zabytki w tym mieście:\n%s\n\n" +
                            "Wymagania dla planu:\n" +
                            "1. Przedstaw plan w postaci sformatowanego dokumentu Markdown z podziałem na każdy dzień (użyj nagłówków drugiego stopnia, np. '## Dzień 1', '## Dzień 2' itd.).\n" +
                            "2. Dla każdego dnia rozpisz harmonogram podzielony na Rano, Popołudnie i Wieczór (użyj nagłówków trzeciego stopnia, np. '### Rano', '### Popołudnie', '### Wieczór').\n" +
                            "3. Dostosuj intensywność planu do tempa (%s): w tempie luźnym sugeruj mniej atrakcji i więcej odpoczynku, w tempie intensywnym zaplanuj dzień bardziej aktywnie.\n" +
                            "4. Dostosuj sugestie bezpośrednio do aktualnej pogody (np. aktywności pod dachem w razie deszczu, parki przy dobrej pogodzie).\n" +
                            "5. Napisz krótkie podsumowanie z sugestiami co warto ze sobą zabrać w oparciu o pogodę.\n" +
                            "6. Całość napisz w języku polskim.",
                    city,
                    days,
                    translatedPace,
                    temp,
                    weatherDesc,
                    placesInfo == null || placesInfo.isEmpty() ? "(brak zewnętrznych danych - dobierz atrakcje samodzielnie)" : String.join("\n", placesInfo),
                    translatedPace
            );

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            GroqResponse response = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GroqResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                Message message = response.choices().getFirst().message();
                if (message != null && message.content() != null) {
                    return message.content();
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error calling Groq API, falling back to local planner", e);
            return null;
        }
    }

    /**
     * Asks Groq for a list of must-see places in the given city, returned as
     * structured data. On any failure (missing key, network, malformed JSON) an
     * empty list is returned so the caller can degrade gracefully.
     */
    public List<PlaceRecommendation> recommendPlaces(String city, int count) {
        if (isApiKeyMissing()) {
            log.warn("Groq API key is not configured. Returning empty places list.");
            return List.of();
        }

        try {
            String prompt = String.format(
                    "Jesteś ekspertem od turystyki. Wymień %d najważniejszych miejsc, które koniecznie trzeba zobaczyć w mieście %s.\n" +
                            "Zwróć WYŁĄCZNIE poprawny obiekt JSON o dokładnie takiej strukturze:\n" +
                            "{\"places\":[{\"name\":\"nazwa miejsca\",\"category\":\"krótka kategoria\",\"description\":\"jedno zwięzłe zdanie po polsku\"}]}\n" +
                            "Pole 'category' to np. Zabytek, Muzeum, Park, Punkt widokowy. Pole 'description' wyjaśnia krótko dlaczego warto. " +
                            "Nie dodawaj żadnego tekstu poza obiektem JSON.",
                    count,
                    city
            );

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", Map.of("type", "json_object")
            );

            GroqResponse response = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GroqResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                Message message = response.choices().getFirst().message();
                if (message != null && message.content() != null) {
                    PlacesWrapper wrapper = objectMapper.readValue(message.content(), PlacesWrapper.class);
                    if (wrapper != null && wrapper.places() != null) {
                        return wrapper.places();
                    }
                }
            }

            return List.of();
        } catch (Exception e) {
            log.error("Error fetching place recommendations from Groq", e);
            return List.of();
        }
    }

    public record GroqResponse(List<Choice> choices) {}
    public record Choice(Message message) {}
    public record Message(String content) {}
    public record PlacesWrapper(List<PlaceRecommendation> places) {}
}

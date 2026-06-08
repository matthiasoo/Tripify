package com.tripify.backend.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;

    public GeminiClient(
            RestClient restClient,
            @Value("${api.gemini.url}") String apiUrl,
            @Value("${api.gemini.key}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public String generatePlan(String city, int days, String pace, double temp, String weatherDesc, List<String> placesInfo) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("dummy-key")) {
            log.warn("Gemini API key is not configured. Falling back to local rules-based planner.");
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
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            GeminiResponse response = restClient.post()
                    .uri(apiUrl + "?key={key}", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                Candidate candidate = response.candidates().getFirst();
                if (candidate.content() != null
                        && candidate.content().parts() != null
                        && !candidate.content().parts().isEmpty()) {
                    return candidate.content().parts().getFirst().text();
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error calling Gemini API, falling back to local planner", e);
            return null;
        }
    }

    public record GeminiResponse(List<Candidate> candidates) {
    }

    public record Candidate(Content content) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}

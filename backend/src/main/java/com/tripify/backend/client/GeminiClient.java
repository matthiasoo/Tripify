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

    public String generatePlan(String city, double temp, String weatherDesc, List<String> placesInfo) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("dummy-key")) {
            log.warn("Gemini API key is not configured. Falling back to local rules-based planner.");
            return null;
        }

        try {
            String prompt = String.format(
                    "Jesteś profesjonalnym agentem AI wspierającym planowanie podróży. " +
                            "Stwórz spersonalizowany, szczegółowy plan 1-dniowej wycieczki dla miasta: %s.\n" +
                            "Aktualna pogoda: %.1f°C, %s.\n" +
                            "Popularne atrakcje i ciekawe miejsca w mieście pobrane z API (Foursquare):\n%s\n\n" +
                            "Wymagania dla planu:\n" +
                            "1. Przedstaw plan w postaci sformatowanego dokumentu Markdown z podziałem na Rano, Popołudnie i Wieczór.\n" +
                            "2. Dostosuj sugestie bezpośrednio do aktualnej pogody.\n" +
                            "3. Napisz, co warto ze sobą zabrać w oparciu o pogodę.\n" +
                            "4. Odpowiedz po polsku.",
                    city,
                    temp,
                    weatherDesc,
                    String.join("\n", placesInfo)
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

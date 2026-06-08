package com.tripify.ai.client;

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
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public GroqClient(
            RestClient restClient,
            @Value("${api.groq.url}") String apiUrl,
            @Value("${api.groq.key}") String apiKey,
            @Value("${api.groq.model}") String model
    ) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generatePlan(String city, int days, String pace, double temp, String weatherDesc, List<String> placesInfo) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("dummy-key")) {
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

    public record GroqResponse(List<Choice> choices) {}
    public record Choice(Message message) {}
    public record Message(String content) {}
}

package com.tripify.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.ai.dto.PlaceRecommendation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqClientTest {

    private GroqClient clientWithoutKey(String apiKey) {
        return new GroqClient(null, new ObjectMapper(), "https://api.groq.com", apiKey, "test-model");
    }

    @Test
    void recommendPlaces_ReturnsEmptyList_WhenApiKeyMissing() {
        GroqClient groqClient = clientWithoutKey("dummy-key");

        List<PlaceRecommendation> places = groqClient.recommendPlaces("Rome", 6);

        assertTrue(places.isEmpty());
    }

    @Test
    void recommendPlaces_ReturnsEmptyList_WhenApiKeyBlank() {
        GroqClient groqClient = clientWithoutKey("");

        List<PlaceRecommendation> places = groqClient.recommendPlaces("Rome", 6);

        assertTrue(places.isEmpty());
    }

    @Test
    void generatePlan_ReturnsNull_WhenApiKeyMissing() {
        GroqClient groqClient = clientWithoutKey("dummy-key");

        String plan = groqClient.generatePlan("Rome", 3, "relaxed", 20.0, "Sunny", List.of());

        assertNull(plan);
    }
}

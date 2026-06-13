package com.tripify.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.client.RestClient;
import com.tripify.backend.client.OpenWeatherClient;
import com.tripify.backend.dto.*;
import com.tripify.backend.model.SavedTripPlan;
import com.tripify.backend.repository.SavedTripPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripPlannerServiceTest {

    @Mock
    private OpenWeatherClient openWeatherClient;


    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private SavedTripPlanRepository savedTripPlanRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private TripPlannerService tripPlannerService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        tripPlannerService = new TripPlannerService(
                openWeatherClient,
                restClientBuilder,
                savedTripPlanRepository,
                objectMapper
        );
    }

    @Test
    void planTrip_Success_WithGeminiPlan() {
        // Arrange
        String city = "Rome";
        int days = 3;
        String pace = "relaxed";
        Long userId = 1L;

        WeatherDto mockWeather = new WeatherDto(25.0, "Sunny");
        List<PlaceDto> mockPlaces = List.of(
                new PlaceDto("Colosseum", "History", "Piazza del Colosseo"),
                new PlaceDto("Trevi Fountain", "Sightseeing", "Piazza di Trevi")
        );
        String mockPlan = "# Spersonalizowany Plan";

        when(openWeatherClient.getWeatherForCity(city)).thenReturn(mockWeather);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(mockPlan);

        // Act
        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, userId);

        // Assert
        assertNotNull(response);
        assertEquals(city, response.city());
        assertEquals(25.0, response.weather().temperature());
        assertEquals("Sunny", response.weather().description());
        assertEquals(0, response.places().size());
        assertEquals(mockPlan, response.plan());
    }

    @Test
    void planTrip_Success_WithLocalFallbackPlan() {
        // Arrange
        String city = "Rome";
        int days = 2;
        String pace = "intense";
        Long userId = 1L;

        WeatherDto mockWeather = new WeatherDto(15.0, "Rainy");
        List<PlaceDto> mockPlaces = List.of(new PlaceDto("Colosseum", "History", "Piazza del Colosseo"));

        when(openWeatherClient.getWeatherForCity(city)).thenReturn(mockWeather);
        
        when(restClient.post()).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, userId);

        // Assert
        assertNotNull(response);
        assertEquals(city, response.city());
        assertTrue(response.plan().contains("awaryjny plan lokalny"));
    }

    @Test
    void savePlan_Success() {
        // Arrange
        Long userId = 1L;
        WeatherDto weather = new WeatherDto(20.0, "Cloudy");
        List<PlaceDto> places = List.of(new PlaceDto("Colosseum", "History", "Rome"));
        SaveTripPlanRequest request = new SaveTripPlanRequest("Rome", weather, places, "Plan text");

        SavedTripPlan savedTripPlan = new SavedTripPlan(userId, "Rome", 20.0, "Cloudy", "[]", "Plan text");
        ReflectionTestUtils.setField(savedTripPlan, "id", 123L);

        when(savedTripPlanRepository.save(any(SavedTripPlan.class))).thenReturn(savedTripPlan);

        // Act
        SavedTripPlanResponse response = tripPlannerService.savePlan(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(123L, response.id());
        assertEquals("Rome", response.city());
        assertEquals("Plan text", response.plan());
        verify(savedTripPlanRepository, times(1)).save(any(SavedTripPlan.class));
    }

    @Test
    void deleteSavedPlan_Success() {
        // Arrange
        Long userId = 1L;
        Long planId = 1L;
        when(savedTripPlanRepository.existsByIdAndUserId(planId, userId)).thenReturn(true);

        // Act
        tripPlannerService.deleteSavedPlan(userId, planId);

        // Assert
        verify(savedTripPlanRepository, times(1)).deleteById(planId);
    }

    @Test
    void deleteSavedPlan_ThrowsException_WhenNotFound() {
        // Arrange
        Long userId = 1L;
        Long planId = 1L;
        when(savedTripPlanRepository.existsByIdAndUserId(planId, userId)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tripPlannerService.deleteSavedPlan(userId, planId));
        verify(savedTripPlanRepository, never()).deleteById(anyLong());
    }

    @Test
    void getSavedPlans_Success() {
        // Arrange
        Long userId = 1L;
        SavedTripPlan savedPlan = new SavedTripPlan(userId, "Rome", 20.0, "Cloudy", "[]", "Plan text");
        ReflectionTestUtils.setField(savedPlan, "id", 5L);

        when(savedTripPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(savedPlan));

        // Act
        List<SavedTripPlanResponse> result = tripPlannerService.getSavedPlans(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals("Rome", result.get(0).city());
    }
}

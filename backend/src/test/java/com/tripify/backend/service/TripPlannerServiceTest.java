package com.tripify.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.backend.client.FoursquareClient;
import com.tripify.backend.client.GeminiClient;
import com.tripify.backend.client.OpenWeatherClient;
import com.tripify.backend.dto.*;
import com.tripify.backend.model.AppUser;
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
    private FoursquareClient foursquareClient;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private SavedTripPlanRepository savedTripPlanRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private TripPlannerService tripPlannerService;

    @BeforeEach
    void setUp() {
        tripPlannerService = new TripPlannerService(
                openWeatherClient,
                foursquareClient,
                geminiClient,
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
        AppUser user = new AppUser("user@example.com", "User", "hash");

        WeatherDto mockWeather = new WeatherDto(25.0, "Sunny");
        List<PlaceDto> mockPlaces = List.of(
                new PlaceDto("Colosseum", "History", "Piazza del Colosseo"),
                new PlaceDto("Trevi Fountain", "Sightseeing", "Piazza di Trevi")
        );
        String mockPlan = "# Spersonalizowany Plan";

        when(openWeatherClient.getWeatherForCity(city)).thenReturn(mockWeather);
        when(foursquareClient.getPlacesForCity(city)).thenReturn(mockPlaces);
        when(geminiClient.generatePlan(eq(city), eq(days), eq(pace), eq(25.0), eq("Sunny"), anyList()))
                .thenReturn(mockPlan);

        // Act
        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, user);

        // Assert
        assertNotNull(response);
        assertEquals(city, response.city());
        assertEquals(25.0, response.weather().temperature());
        assertEquals("Sunny", response.weather().description());
        assertEquals(2, response.places().size());
        assertEquals(mockPlan, response.plan());
    }

    @Test
    void planTrip_Success_WithLocalFallbackPlan() {
        // Arrange
        String city = "Rome";
        int days = 2;
        String pace = "intense";
        AppUser user = new AppUser("user@example.com", "User", "hash");

        WeatherDto mockWeather = new WeatherDto(15.0, "Rainy");
        List<PlaceDto> mockPlaces = List.of(new PlaceDto("Colosseum", "History", "Piazza del Colosseo"));

        when(openWeatherClient.getWeatherForCity(city)).thenReturn(mockWeather);
        when(foursquareClient.getPlacesForCity(city)).thenReturn(mockPlaces);
        // Gemini fails by returning null or blank plan
        when(geminiClient.generatePlan(anyString(), anyInt(), anyString(), anyDouble(), anyString(), anyList()))
                .thenReturn("");

        // Act
        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, user);

        // Assert
        assertNotNull(response);
        assertEquals(city, response.city());
        assertTrue(response.plan().contains("awaryjny plan lokalny"));
        assertTrue(response.plan().contains("Colosseum"));
    }

    @Test
    void savePlan_Success() {
        // Arrange
        AppUser user = new AppUser("user@example.com", "User", "hash");
        WeatherDto weather = new WeatherDto(20.0, "Cloudy");
        List<PlaceDto> places = List.of(new PlaceDto("Colosseum", "History", "Rome"));
        SaveTripPlanRequest request = new SaveTripPlanRequest("Rome", weather, places, "Plan text");

        SavedTripPlan savedTripPlan = new SavedTripPlan(user, "Rome", 20.0, "Cloudy", "[]", "Plan text");
        ReflectionTestUtils.setField(savedTripPlan, "id", 123L);

        when(savedTripPlanRepository.save(any(SavedTripPlan.class))).thenReturn(savedTripPlan);

        // Act
        SavedTripPlanResponse response = tripPlannerService.savePlan(user, request);

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
        AppUser user = new AppUser("user@example.com", "User", "hash");
        Long planId = 1L;
        when(savedTripPlanRepository.existsByIdAndUser(planId, user)).thenReturn(true);

        // Act
        tripPlannerService.deleteSavedPlan(user, planId);

        // Assert
        verify(savedTripPlanRepository, times(1)).deleteById(planId);
    }

    @Test
    void deleteSavedPlan_ThrowsException_WhenNotFound() {
        // Arrange
        AppUser user = new AppUser("user@example.com", "User", "hash");
        Long planId = 1L;
        when(savedTripPlanRepository.existsByIdAndUser(planId, user)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tripPlannerService.deleteSavedPlan(user, planId));
        verify(savedTripPlanRepository, never()).deleteById(anyLong());
    }

    @Test
    void getSavedPlans_Success() {
        // Arrange
        AppUser user = new AppUser("user@example.com", "User", "hash");
        SavedTripPlan savedPlan = new SavedTripPlan(user, "Rome", 20.0, "Cloudy", "[]", "Plan text");
        ReflectionTestUtils.setField(savedPlan, "id", 5L);

        when(savedTripPlanRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(savedPlan));

        // Act
        List<SavedTripPlanResponse> result = tripPlannerService.getSavedPlans(user);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals("Rome", result.get(0).city());
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, userId);

        assertThat(response).isNotNull();
        assertThat(response.city()).isEqualTo(city);
        assertThat(response.weather().temperature()).isEqualTo(25.0);
        assertThat(response.weather().description()).isEqualTo("Sunny");
        assertThat(response.places()).isEmpty();
        assertThat(response.plan()).isEqualTo(mockPlan);
    }

    @Test
    void planTrip_Success_WithLocalFallbackPlan() {
        String city = "Rome";
        int days = 2;
        String pace = "intense";
        Long userId = 1L;

        WeatherDto mockWeather = new WeatherDto(15.0, "Rainy");
        List<PlaceDto> mockPlaces = List.of(new PlaceDto("Colosseum", "History", "Piazza del Colosseo"));

        when(openWeatherClient.getWeatherForCity(city)).thenReturn(mockWeather);
        
        when(restClient.post()).thenThrow(new RuntimeException("Service unavailable"));

        TripPlanResponse response = tripPlannerService.planTrip(city, days, pace, userId);

        assertThat(response).isNotNull();
        assertThat(response.city()).isEqualTo(city);
        assertThat(response.plan()).contains("awaryjny plan lokalny");
    }

    @Test
    void savePlan_Success() {
        Long userId = 1L;
        WeatherDto weather = new WeatherDto(20.0, "Cloudy");
        List<PlaceDto> places = List.of(new PlaceDto("Colosseum", "History", "Rome"));
        SaveTripPlanRequest request = new SaveTripPlanRequest("Rome", weather, places, "Plan text");

        SavedTripPlan savedTripPlan = new SavedTripPlan(userId, "Rome", 20.0, "Cloudy", "[]", "Plan text");
        ReflectionTestUtils.setField(savedTripPlan, "id", 123L);

        when(savedTripPlanRepository.save(any(SavedTripPlan.class))).thenReturn(savedTripPlan);

        SavedTripPlanResponse response = tripPlannerService.savePlan(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(123L);
        assertThat(response.city()).isEqualTo("Rome");
        assertThat(response.plan()).isEqualTo("Plan text");
        verify(savedTripPlanRepository, times(1)).save(any(SavedTripPlan.class));
    }

    @Test
    void regeneratePlan_OverwritesExistingPlan() {
        Long userId = 1L;
        Long planId = 7L;

        SavedTripPlan existing = new SavedTripPlan(userId, "Rome", 10.0, "Old", "[]", "Old plan");
        ReflectionTestUtils.setField(existing, "id", planId);

        when(savedTripPlanRepository.findByIdAndUserId(planId, userId)).thenReturn(java.util.Optional.of(existing));
        when(openWeatherClient.getWeatherForCity("Rome")).thenReturn(new WeatherDto(28.0, "Sunny"));
        when(savedTripPlanRepository.save(any(SavedTripPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("Regenerated plan");

        SavedTripPlanResponse response = tripPlannerService.regeneratePlan(userId, planId, 4, "intense");

        assertThat(response).isNotNull();
        assertThat(response.city()).isEqualTo("Rome");
        assertThat(response.weather().temperature()).isEqualTo(28.0);
        assertThat(response.plan()).isEqualTo("Regenerated plan");
        verify(savedTripPlanRepository, times(1)).save(existing);
    }

    @Test
    void regeneratePlan_ThrowsException_WhenNotFound() {
        Long userId = 1L;
        Long planId = 7L;
        when(savedTripPlanRepository.findByIdAndUserId(planId, userId)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> tripPlannerService.regeneratePlan(userId, planId, 3, "relaxed"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savedTripPlanRepository, never()).save(any(SavedTripPlan.class));
    }

    @Test
    void deleteSavedPlan_Success() {
        Long userId = 1L;
        Long planId = 1L;
        when(savedTripPlanRepository.existsByIdAndUserId(planId, userId)).thenReturn(true);
        tripPlannerService.deleteSavedPlan(userId, planId);
        verify(savedTripPlanRepository, times(1)).deleteById(planId);
    }

    @Test
    void deleteSavedPlan_ThrowsException_WhenNotFound() {
        Long userId = 1L;
        Long planId = 1L;
        when(savedTripPlanRepository.existsByIdAndUserId(planId, userId)).thenReturn(false);
        assertThatThrownBy(() -> tripPlannerService.deleteSavedPlan(userId, planId))
                .isInstanceOf(IllegalArgumentException.class);
        verify(savedTripPlanRepository, never()).deleteById(anyLong());
    }

    @Test
    void getSavedPlans_Success() {
        Long userId = 1L;
        SavedTripPlan savedPlan = new SavedTripPlan(userId, "Rome", 20.0, "Cloudy", "[]", "Plan text");
        ReflectionTestUtils.setField(savedPlan, "id", 5L);

        when(savedTripPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(savedPlan));

        List<SavedTripPlanResponse> result = tripPlannerService.getSavedPlans(userId);

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(5L);
        assertThat(result.get(0).city()).isEqualTo("Rome");
    }
}

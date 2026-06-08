package com.tripify.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.backend.dto.*;
import com.tripify.backend.model.AppUser;
import com.tripify.backend.service.AuthService;
import com.tripify.backend.service.TripPlannerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripPlannerService tripPlannerService;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testToken = "token123";
    private final String authHeader = "Bearer " + testToken;
    private final AppUser mockUser = new AppUser("user@example.com", "Test User", "hash");

    @Test
    void planTrip_Success() throws Exception {
        when(authService.extractBearerToken(authHeader)).thenReturn(testToken);
        when(authService.findAppUserByToken(testToken)).thenReturn(Optional.of(mockUser));

        TripPlanResponse mockResponse = new TripPlanResponse(
                "Rome",
                new WeatherDto(20.0, "Sunny"),
                List.of(new PlaceDto("Colosseum", "History", "Rome")),
                "Plan contents"
        );

        when(tripPlannerService.planTrip(eq("Rome"), eq(3), eq("relaxed"), any(AppUser.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/trips/plan/Rome")
                        .header("Authorization", authHeader)
                        .param("days", "3")
                        .param("pace", "relaxed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Rome"))
                .andExpect(jsonPath("$.plan").value("Plan contents"));
    }

    @Test
    void planTrip_Unauthorized_WhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/trips/plan/Rome"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Zaloguj się, aby generować plany podróży."));
    }

    @Test
    void getSavedPlans_Success() throws Exception {
        when(authService.extractBearerToken(authHeader)).thenReturn(testToken);
        when(authService.findAppUserByToken(testToken)).thenReturn(Optional.of(mockUser));

        SavedTripPlanResponse mockResponse = new SavedTripPlanResponse(
                1L, "Rome", new WeatherDto(20.0, "Sunny"),
                List.of(), "Plan contents", Instant.now()
        );

        when(tripPlannerService.getSavedPlans(any(AppUser.class)))
                .thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/trips/mine")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].city").value("Rome"));
    }

    @Test
    void savePlan_Success() throws Exception {
        when(authService.extractBearerToken(authHeader)).thenReturn(testToken);
        when(authService.findAppUserByToken(testToken)).thenReturn(Optional.of(mockUser));

        SaveTripPlanRequest request = new SaveTripPlanRequest(
                "Rome", new WeatherDto(20.0, "Sunny"), List.of(), "Plan contents"
        );

        SavedTripPlanResponse mockResponse = new SavedTripPlanResponse(
                1L, "Rome", new WeatherDto(20.0, "Sunny"),
                List.of(), "Plan contents", Instant.now()
        );

        when(tripPlannerService.savePlan(any(AppUser.class), any(SaveTripPlanRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/trips/mine")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.city").value("Rome"));
    }

    @Test
    void deleteSavedPlan_Success() throws Exception {
        when(authService.extractBearerToken(authHeader)).thenReturn(testToken);
        when(authService.findAppUserByToken(testToken)).thenReturn(Optional.of(mockUser));

        doNothing().when(tripPlannerService).deleteSavedPlan(any(AppUser.class), eq(1L));

        mockMvc.perform(delete("/api/v1/trips/mine/1")
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        verify(tripPlannerService, times(1)).deleteSavedPlan(any(AppUser.class), eq(1L));
    }
}

package com.tripify.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripify.backend.config.SecurityConfig;
import com.tripify.backend.dto.*;
import com.tripify.backend.service.TripPlannerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@Import(SecurityConfig.class)
class TripControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripPlannerService tripPlannerService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedUser() {
        return jwt().jwt(token -> token.claim("uid", USER_ID));
    }

    @Test
    void planTrip_Success() throws Exception {
        TripPlanResponse mockResponse = new TripPlanResponse(
                "Rome",
                new WeatherDto(20.0, "Sunny"),
                List.of(new PlaceDto("Colosseum", "History", "Rome")),
                "Plan contents"
        );

        when(tripPlannerService.planTrip(eq("Rome"), eq(3), eq("relaxed"), eq(USER_ID)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/trips/plan/Rome")
                        .with(authenticatedUser())
                        .param("days", "3")
                        .param("pace", "relaxed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Rome"))
                .andExpect(jsonPath("$.plan").value("Plan contents"));
    }

    @Test
    void planTrip_Unauthorized_WhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/trips/plan/Rome"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSavedPlans_Success() throws Exception {
        SavedTripPlanResponse mockResponse = new SavedTripPlanResponse(
                1L, "Rome", new WeatherDto(20.0, "Sunny"),
                List.of(), "Plan contents", Instant.now()
        );

        when(tripPlannerService.getSavedPlans(eq(USER_ID)))
                .thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/trips/mine")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].city").value("Rome"));
    }

    @Test
    void savePlan_Success() throws Exception {
        SaveTripPlanRequest request = new SaveTripPlanRequest(
                "Rome", new WeatherDto(20.0, "Sunny"), List.of(), "Plan contents"
        );

        SavedTripPlanResponse mockResponse = new SavedTripPlanResponse(
                1L, "Rome", new WeatherDto(20.0, "Sunny"),
                List.of(), "Plan contents", Instant.now()
        );

        when(tripPlannerService.savePlan(eq(USER_ID), any(SaveTripPlanRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/trips/mine")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.city").value("Rome"));
    }

    @Test
    void deleteSavedPlan_Success() throws Exception {
        doNothing().when(tripPlannerService).deleteSavedPlan(eq(USER_ID), eq(1L));

        mockMvc.perform(delete("/api/v1/trips/mine/1")
                        .with(authenticatedUser()))
                .andExpect(status().isNoContent());

        verify(tripPlannerService, times(1)).deleteSavedPlan(eq(USER_ID), eq(1L));
    }
}

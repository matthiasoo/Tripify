package com.tripify.backend.controller;

import com.tripify.backend.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
@AutoConfigureMockMvc(addFilters = false)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    @Test
    void getWeather_Success() throws Exception {
        Map<String, Object> mockResponse = Map.of(
                "temp", 22.5,
                "description", "scattered clouds"
        );

        when(weatherService.getWeather(anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/weather")
                        .param("city", "Warsaw")
                        .param("type", "current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temp").value(22.5))
                .andExpect(jsonPath("$.description").value("scattered clouds"));
    }

    @Test
    void getWeather_BadRequest_WhenServiceThrowsIllegalArgument() throws Exception {
        when(weatherService.getWeather(anyString(), any(), any(), any(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Provide either 'city' or both 'lat' and 'lon' parameters."));

        mockMvc.perform(get("/api/v1/weather"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Provide either 'city' or both 'lat' and 'lon' parameters."));
    }

    @Test
    void getWeather_GatewayError_WhenServiceThrowsGeneralException() throws Exception {
        when(weatherService.getWeather(anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API connection timeout"));

        mockMvc.perform(get("/api/v1/weather")
                        .param("city", "Warsaw"))
                .andExpect(status().is(502))
                .andExpect(jsonPath("$.error").value("Failed to fetch weather data."));
    }
}

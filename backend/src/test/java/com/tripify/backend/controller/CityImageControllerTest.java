package com.tripify.backend.controller;

import com.tripify.backend.dto.CityImageResponse;
import com.tripify.backend.service.CityImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CityImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class CityImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CityImageService cityImageService;

    @Test
    void searchCityImages_Success() throws Exception {
        CityImageResponse mockResponse = new CityImageResponse(
                50, 5, List.of(new CityImageResponse.PhotoDto(
                        "photoId", "Paris View",
                        new CityImageResponse.PhotoUrls("raw", "regular", "small", "thumb"),
                        new CityImageResponse.PhotoAuthor("Author Name", "authorUser", "profileUrl"),
                        "blur", "#000000", 800, 600
                ))
        );

        when(cityImageService.search(eq("Paris"), anyInt(), anyInt(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/city-images")
                        .param("city", "Paris")
                        .param("page", "1")
                        .param("per_page", "10")
                        .param("orientation", "landscape"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(50))
                .andExpect(jsonPath("$.photos[0].id").value("photoId"))
                .andExpect(jsonPath("$.photos[0].description").value("Paris View"));
    }

    @Test
    void searchCityImages_GatewayError_WhenServiceThrowsException() throws Exception {
        when(cityImageService.search(anyString(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Unsplash API is down"));

        mockMvc.perform(get("/api/v1/city-images")
                        .param("city", "Paris"))
                .andExpect(status().is(502))
                .andExpect(jsonPath("$.error").value("Nie udało się pobrać zdjęć miasta."));
    }
}

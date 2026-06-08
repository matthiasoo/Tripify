package com.tripify.backend.service;

import com.tripify.backend.client.UnsplashClient;
import com.tripify.backend.dto.CityImageResponse;
import com.tripify.backend.dto.UnsplashSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityImageServiceTest {

    @Mock
    private UnsplashClient unsplashClient;

    @InjectMocks
    private CityImageService cityImageService;

    @Test
    void search_Success() {
        // Arrange
        String city = "Paris";
        int page = 1;
        int perPage = 10;
        String orientation = "landscape";

        UnsplashSearchResponse.UnsplashUrls urls = new UnsplashSearchResponse.UnsplashUrls(
                "raw_url", "reg_url", "small_url", "thumb_url"
        );
        UnsplashSearchResponse.UnsplashLinks links = new UnsplashSearchResponse.UnsplashLinks("html_link");
        UnsplashSearchResponse.UnsplashUser user = new UnsplashSearchResponse.UnsplashUser(
                "John Doe", "johndoe", links
        );
        UnsplashSearchResponse.UnsplashPhoto photo = new UnsplashSearchResponse.UnsplashPhoto(
                "photo1", "Eiffel Tower", "alt_desc", urls, user, "blur", "#ffffff", 1920, 1080
        );
        UnsplashSearchResponse mockResponse = new UnsplashSearchResponse(100, 10, List.of(photo));

        when(unsplashClient.searchPhotos(city, page, perPage, orientation)).thenReturn(mockResponse);

        // Act
        CityImageResponse result = cityImageService.search(city, page, perPage, orientation);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.total());
        assertEquals(10, result.totalPages());
        assertEquals(1, result.photos().size());

        CityImageResponse.PhotoDto resultPhoto = result.photos().get(0);
        assertEquals("photo1", resultPhoto.id());
        assertEquals("Eiffel Tower", resultPhoto.description());
        assertEquals("raw_url", resultPhoto.urls().raw());
        assertEquals("reg_url", resultPhoto.urls().regular());
        assertEquals("John Doe", resultPhoto.author().name());
        assertEquals("html_link", resultPhoto.author().profileUrl());
    }

    @Test
    void search_FallbackDescription() {
        // Arrange
        String city = "Paris";
        UnsplashSearchResponse.UnsplashUrls urls = new UnsplashSearchResponse.UnsplashUrls("r", "rg", "s", "t");
        UnsplashSearchResponse.UnsplashUser user = new UnsplashSearchResponse.UnsplashUser("User", "user", null);
        
        // Photo with description = null, relying on altDescription
        UnsplashSearchResponse.UnsplashPhoto photo = new UnsplashSearchResponse.UnsplashPhoto(
                "photo1", null, "Alternative Description", urls, user, "blur", "#ffffff", 1920, 1080
        );
        UnsplashSearchResponse mockResponse = new UnsplashSearchResponse(1, 1, List.of(photo));

        when(unsplashClient.searchPhotos(anyString(), anyInt(), anyInt(), anyString())).thenReturn(mockResponse);

        // Act
        CityImageResponse result = cityImageService.search(city, 1, 1, "landscape");

        // Assert
        assertEquals("Alternative Description", result.photos().get(0).description());
    }
}

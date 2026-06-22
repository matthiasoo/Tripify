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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityImageServiceTest {

    @Mock
    private UnsplashClient unsplashClient;

    @InjectMocks
    private CityImageService cityImageService;

    @Test
    void search_Success() {
        String city = "Paris";
        int page = 1;
        int perPage = 10;
        String orientation = "landscape";

        UnsplashSearchResponse.UnsplashUrls urls = new UnsplashSearchResponse.UnsplashUrls(
                "raw_url", "reg_url", "small_url", "thumb_url"
        );
        UnsplashSearchResponse.UnsplashLinks links = new UnsplashSearchResponse.UnsplashLinks("html_link");
        UnsplashSearchResponse.UnsplashUser user = new UnsplashSearchResponse.UnsplashUser(
                "Jimmy Gonzales", "jimmygonzales", links
        );
        UnsplashSearchResponse.UnsplashPhoto photo = new UnsplashSearchResponse.UnsplashPhoto(
                "photo1", "Eiffel Tower", "alt_desc", urls, user, "blur", "#ffffff", 1920, 1080
        );
        UnsplashSearchResponse mockResponse = new UnsplashSearchResponse(100, 10, List.of(photo));

        when(unsplashClient.searchPhotos(city, page, perPage, orientation)).thenReturn(mockResponse);


        CityImageResponse result = cityImageService.search(city, page, perPage, orientation);


        assertThat(result).isNotNull();
        assertThat(result.total()).isEqualTo(100);
        assertThat(result.totalPages()).isEqualTo(10);
        assertThat(result.photos()).hasSize(1);

        CityImageResponse.PhotoDto resultPhoto = result.photos().get(0);
        assertThat(resultPhoto.id()).isEqualTo("photo1");
        assertThat(resultPhoto.description()).isEqualTo("Eiffel Tower");
        assertThat(resultPhoto.urls().raw()).isEqualTo("raw_url");
        assertThat(resultPhoto.urls().regular()).isEqualTo("reg_url");
        assertThat(resultPhoto.author().name()).isEqualTo("Jimmy Gonzales");
        assertThat(resultPhoto.author().profileUrl()).isEqualTo("html_link");
    }

    @Test
    void search_FallbackDescription() {
        String city = "Paris";
        UnsplashSearchResponse.UnsplashUrls urls = new UnsplashSearchResponse.UnsplashUrls("r", "rg", "s", "t");
        UnsplashSearchResponse.UnsplashUser user = new UnsplashSearchResponse.UnsplashUser("User", "user", null);
        
        UnsplashSearchResponse.UnsplashPhoto photo = new UnsplashSearchResponse.UnsplashPhoto(
                "photo1", null, "Alternative Description", urls, user, "blur", "#ffffff", 1920, 1080
        );
        UnsplashSearchResponse mockResponse = new UnsplashSearchResponse(1, 1, List.of(photo));

        when(unsplashClient.searchPhotos(anyString(), anyInt(), anyInt(), anyString())).thenReturn(mockResponse);

        CityImageResponse result = cityImageService.search(city, 1, 1, "landscape");
        assertThat(result.photos().get(0).description()).isEqualTo("Alternative Description");
    }
}

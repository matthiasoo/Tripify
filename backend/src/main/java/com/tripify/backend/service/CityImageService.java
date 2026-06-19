package com.tripify.backend.service;

import com.tripify.backend.client.UnsplashClient;
import com.tripify.backend.dto.CityImageResponse;
import com.tripify.backend.dto.UnsplashSearchResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityImageService {

    private final UnsplashClient unsplashClient;

    public CityImageService(UnsplashClient unsplashClient) {
        this.unsplashClient = unsplashClient;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CityImageService.class);

    public CityImageResponse search(String city, int page, int perPage, String orientation) {
        try {
            UnsplashSearchResponse raw = unsplashClient.searchPhotos(city, page, perPage, orientation);

            List<CityImageResponse.PhotoDto> photos = raw.results().stream()
                    .map(photo -> new CityImageResponse.PhotoDto(
                            photo.id(),
                            photo.description() != null ? photo.description() : photo.altDescription(),
                            new CityImageResponse.PhotoUrls(
                                    photo.urls().raw(),
                                    photo.urls().regular(),
                                    photo.urls().small(),
                                    photo.urls().thumb()
                            ),
                            new CityImageResponse.PhotoAuthor(
                                    photo.user().name(),
                                    photo.user().username(),
                                    photo.user().links() != null ? photo.user().links().html() : null
                            ),
                            photo.blurHash(),
                            photo.color(),
                            photo.width(),
                            photo.height()
                    ))
                    .toList();

            return new CityImageResponse(raw.total(), raw.totalPages(), photos);
        } catch (Exception e) {
            log.warn("Failed to fetch images from Unsplash for city {}: {}", city, e.getMessage());
            return new CityImageResponse(0, 0, List.of());
        }
    }
}

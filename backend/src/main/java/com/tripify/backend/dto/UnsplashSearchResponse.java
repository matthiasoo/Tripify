package com.tripify.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UnsplashSearchResponse(
        int total,
        @JsonProperty("total_pages") int totalPages,
        List<UnsplashPhoto> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnsplashPhoto(
            String id,
            String description,
            @JsonProperty("alt_description") String altDescription,
            UnsplashUrls urls,
            UnsplashUser user,
            @JsonProperty("blur_hash") String blurHash,
            String color,
            int width,
            int height
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnsplashUrls(
            String raw,
            String regular,
            String small,
            String thumb
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnsplashUser(
            String name,
            String username,
            UnsplashLinks links
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnsplashLinks(
            String html
    ) {}
}

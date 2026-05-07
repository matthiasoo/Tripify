package com.tripify.backend.dto;

import java.util.List;

public record CityImageResponse(
        int total,
        int totalPages,
        List<PhotoDto> photos
) {
    public record PhotoDto(
            String id,
            String description,
            PhotoUrls urls,
            PhotoAuthor author,
            String blurHash,
            String color,
            int width,
            int height
    ) {}

    public record PhotoUrls(
            String raw,
            String regular,
            String small,
            String thumb
    ) {}

    public record PhotoAuthor(
            String name,
            String username,
            String profileUrl
    ) {}
}

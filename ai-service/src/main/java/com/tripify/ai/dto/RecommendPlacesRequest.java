package com.tripify.ai.dto;

public record RecommendPlacesRequest(
        String city,
        Integer count
) {
}

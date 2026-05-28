package com.tripify.backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}

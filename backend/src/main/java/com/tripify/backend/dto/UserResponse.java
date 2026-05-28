package com.tripify.backend.dto;

public record UserResponse(
        Long id,
        String name,
        String email
) {
}

package com.tripify.auth.dto;

public record UserResponse(
        Long id,
        String name,
        String email
) {
}

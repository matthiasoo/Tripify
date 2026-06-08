package com.tripify.ai.dto;

import java.util.List;

public record GeneratePlanRequest(
        String city,
        int days,
        String pace,
        double temperature,
        String weatherDescription,
        List<String> placesInfo
) {
}

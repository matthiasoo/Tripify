package com.tripify.backend.dto;

import java.util.List;

public record OpenWeatherResponse(
        List<WeatherDesc> weather,
        MainData main
) {
    public record WeatherDesc(String description) {}
    public record MainData(double temp) {}
}

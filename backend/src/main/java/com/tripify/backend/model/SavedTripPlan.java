package com.tripify.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "saved_trip_plans")
public class SavedTripPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false)
    private double weatherTemperature;

    @Column(nullable = false, length = 255)
    private String weatherDescription;

    @Lob
    @Column(nullable = false)
    private String placesJson;

    @Lob
    @Column(nullable = false)
    private String plan;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected SavedTripPlan() {
    }

    public SavedTripPlan(
            AppUser user,
            String city,
            double weatherTemperature,
            String weatherDescription,
            String placesJson,
            String plan
    ) {
        this.user = user;
        this.city = city;
        this.weatherTemperature = weatherTemperature;
        this.weatherDescription = weatherDescription;
        this.placesJson = placesJson;
        this.plan = plan;
    }

    public Long getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public double getWeatherTemperature() {
        return weatherTemperature;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public String getPlacesJson() {
        return placesJson;
    }

    public String getPlan() {
        return plan;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

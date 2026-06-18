package com.tripify.backend.repository;

import com.tripify.backend.model.SavedTripPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedTripPlanRepository extends JpaRepository<SavedTripPlan, Long> {
    List<SavedTripPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    Optional<SavedTripPlan> findByIdAndUserId(Long id, Long userId);
}

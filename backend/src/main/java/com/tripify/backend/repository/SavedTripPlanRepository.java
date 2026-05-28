package com.tripify.backend.repository;

import com.tripify.backend.model.AppUser;
import com.tripify.backend.model.SavedTripPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedTripPlanRepository extends JpaRepository<SavedTripPlan, Long> {
    List<SavedTripPlan> findByUserOrderByCreatedAtDesc(AppUser user);

    boolean existsByIdAndUser(Long id, AppUser user);
}

package com.itercraft.api.domain.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ActivityAnalysisRepository extends JpaRepository<ActivityAnalysis, UUID> {
    Optional<ActivityAnalysis> findByLocationNameAndAnalysisDate(String locationName, LocalDate analysisDate);
}

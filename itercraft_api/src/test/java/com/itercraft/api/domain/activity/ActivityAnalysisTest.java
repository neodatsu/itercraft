package com.itercraft.api.domain.activity;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityAnalysisTest {

    @Test
    void constructor_shouldInitializeAllFields() {
        ActivityAnalysis analysis = new ActivityAnalysis(
                "Montpellier", 43.6108, 3.8767, "{\"result\":\"sunny\"}");

        assertThat(analysis.getId()).isNotNull();
        assertThat(analysis.getLocationName()).isEqualTo("Montpellier");
        assertThat(analysis.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(43.6108));
        assertThat(analysis.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(3.8767));
        assertThat(analysis.getAnalysisDate()).isEqualTo(LocalDate.now());
        assertThat(analysis.getResponseJson()).isEqualTo("{\"result\":\"sunny\"}");
        assertThat(analysis.getCreatedAt()).isNotNull();
    }

    @Test
    void constructor_shouldGenerateUniqueIds() {
        ActivityAnalysis a1 = new ActivityAnalysis("A", 0, 0, "{}");
        ActivityAnalysis a2 = new ActivityAnalysis("B", 0, 0, "{}");

        assertThat(a1.getId()).isNotEqualTo(a2.getId());
    }
}

package com.itercraft.api.infrastructure.web.dto;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsageDtoTest {

    @Test
    void record_shouldStoreAndReturnValues() {
        UUID id = UUID.randomUUID();
        OffsetDateTime usedAt = OffsetDateTime.now();

        UsageDto dto = new UsageDto(id, usedAt);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.usedAt()).isEqualTo(usedAt);
    }

    @Test
    void equals_shouldWorkCorrectly() {
        UUID id = UUID.randomUUID();
        OffsetDateTime usedAt = OffsetDateTime.now();

        UsageDto dto1 = new UsageDto(id, usedAt);
        UsageDto dto2 = new UsageDto(id, usedAt);

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void toString_shouldContainFieldValues() {
        UUID id = UUID.randomUUID();
        OffsetDateTime usedAt = OffsetDateTime.now();

        UsageDto dto = new UsageDto(id, usedAt);

        assertThat(dto.toString()).contains(id.toString());
    }
}

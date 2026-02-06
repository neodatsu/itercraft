package com.itercraft.api.infrastructure.web.dto;

import java.time.OffsetDateTime;

public record MaintenanceActivityDto(
    String serviceCode,
    String serviceLabel,
    boolean isActive,
    OffsetDateTime startedAt,
    int totalMinutesToday
) {}

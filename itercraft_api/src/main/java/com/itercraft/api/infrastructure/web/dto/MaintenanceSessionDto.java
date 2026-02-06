package com.itercraft.api.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceSessionDto(
    UUID id,
    String serviceCode,
    String serviceLabel,
    OffsetDateTime startedAt,
    OffsetDateTime endedAt,
    Integer durationMinutes,
    boolean autoStopped
) {}

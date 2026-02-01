package com.itercraft.api.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsageDto(UUID id, OffsetDateTime usedAt) {}

package com.itercraft.api.infrastructure.web.dto;

import java.util.List;

public record MaintenanceTotalsDto(
    int todayMinutes,
    int weekMinutes,
    int monthMinutes,
    int yearMinutes,
    List<ActivityTotalsDto> byActivity
) {}

package com.itercraft.api.infrastructure.web.dto;

public record ActivityTotalsDto(
    String serviceCode,
    String serviceLabel,
    int todayMinutes,
    int weekMinutes,
    int monthMinutes,
    int yearMinutes
) {}

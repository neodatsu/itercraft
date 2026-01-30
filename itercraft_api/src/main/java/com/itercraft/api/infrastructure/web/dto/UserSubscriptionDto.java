package com.itercraft.api.infrastructure.web.dto;

public record UserSubscriptionDto(String serviceCode, String serviceLabel, long usageCount) {}

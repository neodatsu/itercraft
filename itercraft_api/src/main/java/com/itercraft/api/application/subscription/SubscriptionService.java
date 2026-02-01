package com.itercraft.api.application.subscription;

import com.itercraft.api.infrastructure.web.dto.ServiceDto;
import com.itercraft.api.infrastructure.web.dto.UsageDto;
import com.itercraft.api.infrastructure.web.dto.UserSubscriptionDto;
import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    void subscribe(String keycloakSub, String serviceCode);
    void unsubscribe(String keycloakSub, String serviceCode);
    void addUsage(String keycloakSub, String serviceCode);
    void removeUsage(String keycloakSub, String serviceCode, UUID usageId);
    List<UserSubscriptionDto> getUserSubscriptions(String keycloakSub);
    List<ServiceDto> getAllServices();
    List<UsageDto> getUsageHistory(String keycloakSub, String serviceCode);
}

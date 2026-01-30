package com.itercraft.api.application.subscription;

import com.itercraft.api.infrastructure.web.dto.ServiceDto;
import com.itercraft.api.infrastructure.web.dto.UserSubscriptionDto;
import java.util.List;

public interface SubscriptionService {
    void subscribe(String keycloakSub, String serviceCode);
    void unsubscribe(String keycloakSub, String serviceCode);
    void addUsage(String keycloakSub, String serviceCode);
    void removeUsage(String keycloakSub, String serviceCode);
    List<UserSubscriptionDto> getUserSubscriptions(String keycloakSub);
    List<ServiceDto> getAllServices();
}

package com.itercraft.api.application.subscription;

import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.AppUserRepository;
import com.itercraft.api.domain.subscription.ServiceEntity;
import com.itercraft.api.domain.subscription.ServiceRepository;
import com.itercraft.api.domain.subscription.ServiceUsage;
import com.itercraft.api.domain.subscription.ServiceUsageRepository;
import com.itercraft.api.domain.subscription.Subscription;
import com.itercraft.api.domain.subscription.SubscriptionRepository;
import com.itercraft.api.infrastructure.web.dto.ServiceDto;
import com.itercraft.api.infrastructure.web.dto.UserSubscriptionDto;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final AppUserRepository appUserRepository;
    private final ServiceRepository serviceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ServiceUsageRepository serviceUsageRepository;

    public SubscriptionServiceImpl(AppUserRepository appUserRepository,
                                   ServiceRepository serviceRepository,
                                   SubscriptionRepository subscriptionRepository,
                                   ServiceUsageRepository serviceUsageRepository) {
        this.appUserRepository = appUserRepository;
        this.serviceRepository = serviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.serviceUsageRepository = serviceUsageRepository;
    }

    @Override
    public void subscribe(String keycloakSub, String serviceCode) {
        AppUser user = findOrCreateUser(keycloakSub);
        ServiceEntity service = findService(serviceCode);
        if (subscriptionRepository.findByUserAndService(user, service).isPresent()) {
            throw new IllegalStateException("Already subscribed to service: " + serviceCode);
        }
        subscriptionRepository.save(new Subscription(user, service));
    }

    @Override
    public void unsubscribe(String keycloakSub, String serviceCode) {
        AppUser user = findUser(keycloakSub);
        ServiceEntity service = findService(serviceCode);
        subscriptionRepository.deleteByUserAndService(user, service);
    }

    @Override
    public void addUsage(String keycloakSub, String serviceCode) {
        Subscription subscription = findSubscription(keycloakSub, serviceCode);
        serviceUsageRepository.save(new ServiceUsage(subscription));
    }

    @Override
    public void removeUsage(String keycloakSub, String serviceCode) {
        Subscription subscription = findSubscription(keycloakSub, serviceCode);
        ServiceUsage usage = serviceUsageRepository
                .findFirstBySubscriptionOrderByUsedAtDesc(subscription)
                .orElseThrow(() -> new IllegalStateException("No usage to remove for service: " + serviceCode));
        serviceUsageRepository.delete(usage);
    }

    @Override
    public List<UserSubscriptionDto> getUserSubscriptions(String keycloakSub) {
        return appUserRepository.findByKeycloakSub(keycloakSub)
                .map(user -> subscriptionRepository.findByUser(user).stream()
                        .map(sub -> new UserSubscriptionDto(
                                sub.getService().getCode(),
                                sub.getService().getLabel(),
                                serviceUsageRepository.countBySubscription(sub)))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<ServiceDto> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(s -> new ServiceDto(s.getCode(), s.getLabel(), s.getDescription()))
                .toList();
    }

    private AppUser findOrCreateUser(String keycloakSub) {
        return appUserRepository.findByKeycloakSub(keycloakSub)
                .orElseGet(() -> appUserRepository.save(new AppUser(keycloakSub)));
    }

    private AppUser findUser(String keycloakSub) {
        return appUserRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private ServiceEntity findService(String serviceCode) {
        return serviceRepository.findByCode(serviceCode)
                .orElseThrow(() -> new IllegalStateException("Service not found: " + serviceCode));
    }

    private Subscription findSubscription(String keycloakSub, String serviceCode) {
        AppUser user = findUser(keycloakSub);
        ServiceEntity service = findService(serviceCode);
        return subscriptionRepository.findByUserAndService(user, service)
                .orElseThrow(() -> new IllegalStateException("Not subscribed to service: " + serviceCode));
    }
}

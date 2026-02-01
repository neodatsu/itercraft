package com.itercraft.api.application.subscription;

import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.AppUserRepository;
import com.itercraft.api.domain.subscription.ServiceEntity;
import com.itercraft.api.domain.subscription.ServiceRepository;
import com.itercraft.api.domain.subscription.ServiceUsage;
import com.itercraft.api.domain.subscription.ServiceUsageRepository;
import com.itercraft.api.domain.subscription.Subscription;
import com.itercraft.api.domain.subscription.SubscriptionRepository;
import com.itercraft.api.infrastructure.sse.SseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ServiceUsageRepository serviceUsageRepository;
    @Mock private SseService sseService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private static final String SUB = "user-sub-123";
    private static final String SERVICE_CODE = "tondeuse";

    @Test
    void subscribe_shouldCreateSubscription() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        subscriptionService.subscribe(SUB, SERVICE_CODE);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(sseService).broadcast("subscription-change");
    }

    @Test
    void subscribe_shouldThrowWhenAlreadySubscribed() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service))
                .thenReturn(Optional.of(new Subscription(user, service)));

        assertThatThrownBy(() -> subscriptionService.subscribe(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unsubscribe_shouldDelete() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));

        subscriptionService.unsubscribe(SUB, SERVICE_CODE);

        verify(subscriptionRepository).deleteByUserAndService(user, service);
        verify(sseService).broadcast("subscription-change");
    }

    @Test
    void addUsage_shouldCreateUsage() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        Subscription subscription = new Subscription(user, service);
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(subscription));
        when(serviceUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        subscriptionService.addUsage(SUB, SERVICE_CODE);

        verify(serviceUsageRepository).save(any(ServiceUsage.class));
        verify(sseService).broadcast("subscription-change");
    }

    @Test
    void removeUsage_shouldDeleteById() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        Subscription subscription = new Subscription(user, service);
        ServiceUsage usage = new ServiceUsage(subscription);
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(subscription));
        when(serviceUsageRepository.findById(usage.getId())).thenReturn(Optional.of(usage));

        subscriptionService.removeUsage(SUB, SERVICE_CODE, usage.getId());

        verify(serviceUsageRepository).delete(usage);
        verify(sseService).broadcast("subscription-change");
    }

    @Test
    void removeUsage_shouldThrowWhenUsageNotFound() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        Subscription subscription = new Subscription(user, service);
        var fakeId = java.util.UUID.randomUUID();
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(subscription));
        when(serviceUsageRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.removeUsage(SUB, SERVICE_CODE, fakeId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void subscribe_shouldThrowWhenServiceNotFound() {
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(new AppUser(SUB)));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.subscribe(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class);
    }

    private ServiceEntity mockService() {
        return mock(ServiceEntity.class);
    }
}

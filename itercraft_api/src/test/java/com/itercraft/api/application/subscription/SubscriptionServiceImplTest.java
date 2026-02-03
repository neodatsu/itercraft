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
import com.itercraft.api.infrastructure.web.dto.ServiceDto;
import com.itercraft.api.infrastructure.web.dto.UsageDto;
import com.itercraft.api.infrastructure.web.dto.UserSubscriptionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void subscribe_shouldCreateUserWhenNotExists() {
        ServiceEntity service = mockService();
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        subscriptionService.subscribe(SUB, SERVICE_CODE);

        verify(appUserRepository).save(any(AppUser.class));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void getUserSubscriptions_shouldReturnEmptyListWhenUserNotFound() {
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());

        List<UserSubscriptionDto> result = subscriptionService.getUserSubscriptions(SUB);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserSubscriptions_shouldReturnSubscriptions() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Tondeuse");
        Subscription subscription = new Subscription(user, service);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(List.of(subscription));
        when(serviceUsageRepository.countBySubscription(subscription)).thenReturn(5L);

        List<UserSubscriptionDto> result = subscriptionService.getUserSubscriptions(SUB);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).serviceCode()).isEqualTo(SERVICE_CODE);
        assertThat(result.get(0).serviceLabel()).isEqualTo("Tondeuse");
        assertThat(result.get(0).usageCount()).isEqualTo(5);
    }

    @Test
    void getUsageHistory_shouldReturnUsages() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        Subscription subscription = new Subscription(user, service);
        ServiceUsage usage = new ServiceUsage(subscription);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(subscription));
        when(serviceUsageRepository.findBySubscriptionOrderByUsedAtDesc(subscription)).thenReturn(List.of(usage));

        List<UsageDto> result = subscriptionService.getUsageHistory(SUB, SERVICE_CODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(usage.getId());
        assertThat(result.get(0).usedAt()).isEqualTo(usage.getUsedAt());
    }

    @Test
    void getUsageHistory_shouldThrowWhenNotSubscribed() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getUsageHistory(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not subscribed");
    }

    @Test
    void getAllServices_shouldReturnAllServices() {
        ServiceEntity service1 = mockService();
        when(service1.getCode()).thenReturn("tondeuse");
        when(service1.getLabel()).thenReturn("Tondeuse");
        when(service1.getDescription()).thenReturn("Service tondeuse");

        ServiceEntity service2 = mockService();
        when(service2.getCode()).thenReturn("arrosage");
        when(service2.getLabel()).thenReturn("Arrosage");
        when(service2.getDescription()).thenReturn("Service arrosage");

        when(serviceRepository.findAll()).thenReturn(List.of(service1, service2));

        List<ServiceDto> result = subscriptionService.getAllServices();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("tondeuse");
        assertThat(result.get(0).label()).isEqualTo("Tondeuse");
        assertThat(result.get(0).description()).isEqualTo("Service tondeuse");
        assertThat(result.get(1).code()).isEqualTo("arrosage");
    }

    @Test
    void unsubscribe_shouldThrowWhenUserNotFound() {
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.unsubscribe(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void addUsage_shouldThrowWhenNotSubscribed() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.addUsage(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not subscribed");
    }

    @Test
    void removeUsage_shouldThrowWhenUsageDoesNotBelongToSubscription() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        Subscription subscription = new Subscription(user, service);

        // Create another subscription to create usage with different subscription
        AppUser otherUser = new AppUser("other-user");
        Subscription otherSubscription = new Subscription(otherUser, service);
        ServiceUsage usage = new ServiceUsage(otherSubscription);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(subscription));
        when(serviceUsageRepository.findById(usage.getId())).thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> subscriptionService.removeUsage(SUB, SERVICE_CODE, usage.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Usage does not belong to this subscription");
    }

    private ServiceEntity mockService() {
        return mock(ServiceEntity.class);
    }
}

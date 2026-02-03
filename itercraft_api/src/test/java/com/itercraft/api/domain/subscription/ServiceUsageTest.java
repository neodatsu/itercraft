package com.itercraft.api.domain.subscription;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ServiceUsageTest {

    @Test
    void constructor_shouldInitializeAllFields() {
        AppUser user = new AppUser("user-123");
        ServiceEntity service = mock(ServiceEntity.class);
        Subscription subscription = new Subscription(user, service);

        ServiceUsage usage = new ServiceUsage(subscription);

        assertThat(usage.getId()).isNotNull();
        assertThat(usage.getSubscription()).isEqualTo(subscription);
        assertThat(usage.getUsedAt()).isNotNull();
    }

    @Test
    void constructor_shouldGenerateUniqueIds() {
        AppUser user = new AppUser("user-123");
        ServiceEntity service = mock(ServiceEntity.class);
        Subscription subscription = new Subscription(user, service);

        ServiceUsage usage1 = new ServiceUsage(subscription);
        ServiceUsage usage2 = new ServiceUsage(subscription);

        assertThat(usage1.getId()).isNotEqualTo(usage2.getId());
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        Subscription subscription = new Subscription(user, service);

        ServiceUsage usage = new ServiceUsage(subscription);

        assertThat(usage.getSubscription()).isSameAs(subscription);
        assertThat(usage.getId()).isNotNull();
        assertThat(usage.getUsedAt()).isNotNull();
    }
}

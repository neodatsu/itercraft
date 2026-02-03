package com.itercraft.api.domain.subscription;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SubscriptionTest {

    @Test
    void constructor_shouldInitializeAllFields() {
        AppUser user = new AppUser("user-123");
        ServiceEntity service = mock(ServiceEntity.class);

        Subscription subscription = new Subscription(user, service);

        assertThat(subscription.getId()).isNotNull();
        assertThat(subscription.getUser()).isEqualTo(user);
        assertThat(subscription.getService()).isEqualTo(service);
        assertThat(subscription.getSubscribedAt()).isNotNull();
    }

    @Test
    void constructor_shouldGenerateUniqueIds() {
        AppUser user = new AppUser("user-123");
        ServiceEntity service = mock(ServiceEntity.class);

        Subscription sub1 = new Subscription(user, service);
        Subscription sub2 = new Subscription(user, service);

        assertThat(sub1.getId()).isNotEqualTo(sub2.getId());
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);

        Subscription subscription = new Subscription(user, service);

        assertThat(subscription.getUser()).isSameAs(user);
        assertThat(subscription.getService()).isSameAs(service);
        assertThat(subscription.getId()).isNotNull();
        assertThat(subscription.getSubscribedAt()).isNotNull();
    }
}

package com.itercraft.api.domain.subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserAndService(AppUser user, ServiceEntity service);
    List<Subscription> findByUser(AppUser user);
    void deleteByUserAndService(AppUser user, ServiceEntity service);
}

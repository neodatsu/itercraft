package com.itercraft.api.domain.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceUsageRepository extends JpaRepository<ServiceUsage, UUID> {
    Optional<ServiceUsage> findFirstBySubscriptionOrderByUsedAtDesc(Subscription subscription);
    long countBySubscription(Subscription subscription);
}

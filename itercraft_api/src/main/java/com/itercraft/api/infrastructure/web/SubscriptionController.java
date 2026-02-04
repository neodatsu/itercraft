package com.itercraft.api.infrastructure.web;

import com.itercraft.api.application.subscription.SubscriptionService;
import com.itercraft.api.infrastructure.web.dto.ServiceDto;
import com.itercraft.api.infrastructure.web.dto.UsageDto;
import com.itercraft.api.infrastructure.web.dto.UserSubscriptionDto;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<UserSubscriptionDto>> getUserSubscriptions(BearerTokenAuthentication token) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptions(extractSub(token)));
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        return ResponseEntity.ok(subscriptionService.getAllServices());
    }

    @PostMapping("/subscriptions/{serviceCode}")
    public ResponseEntity<Void> subscribe(@PathVariable String serviceCode,
                                          BearerTokenAuthentication token) {
        subscriptionService.subscribe(extractSub(token), serviceCode);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/subscriptions/{serviceCode}")
    public ResponseEntity<Void> unsubscribe(@PathVariable String serviceCode,
                                            BearerTokenAuthentication token) {
        subscriptionService.unsubscribe(extractSub(token), serviceCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriptions/{serviceCode}/usages")
    public ResponseEntity<List<UsageDto>> getUsageHistory(@PathVariable String serviceCode,
                                                          BearerTokenAuthentication token) {
        return ResponseEntity.ok(subscriptionService.getUsageHistory(extractSub(token), serviceCode));
    }

    @PostMapping("/subscriptions/{serviceCode}/usages")
    public ResponseEntity<Void> addUsage(@PathVariable String serviceCode,
                                         BearerTokenAuthentication token) {
        subscriptionService.addUsage(extractSub(token), serviceCode);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/subscriptions/{serviceCode}/usages/{usageId}")
    public ResponseEntity<Void> removeUsage(@PathVariable String serviceCode,
                                            @PathVariable UUID usageId,
                                            BearerTokenAuthentication token) {
        subscriptionService.removeUsage(extractSub(token), serviceCode, usageId);
        return ResponseEntity.noContent().build();
    }

    private String extractSub(BearerTokenAuthentication token) {
        // For opaque tokens, 'sub' is in token attributes, not in getName()
        Object sub = token.getTokenAttributes().get("sub");
        if (sub == null) {
            throw new IllegalStateException("Token does not contain 'sub' claim");
        }
        return sub.toString();
    }
}

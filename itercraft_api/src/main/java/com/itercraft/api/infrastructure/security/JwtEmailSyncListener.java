package com.itercraft.api.infrastructure.security;

import com.itercraft.api.domain.subscription.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtEmailSyncListener {

    private final AppUserRepository appUserRepository;

    public JwtEmailSyncListener(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    @EventListener
    public void onAuthentication(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication() instanceof JwtAuthenticationToken jwt)) {
            return;
        }
        String sub = jwt.getToken().getSubject();
        String email = (String) jwt.getToken().getClaim("email");
        if (sub == null || email == null) {
            return;
        }
        appUserRepository.findByKeycloakSub(sub)
                .filter(user -> user.getEmail() == null)
                .ifPresent(user -> user.setEmail(email));
    }
}

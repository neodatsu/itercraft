package com.itercraft.api.infrastructure.security;

import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.AppUserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtEmailSyncListenerTest {

    @Mock
    private AppUserRepository appUserRepository;

    private JwtEmailSyncListener listener;

    @BeforeEach
    void setUp() {
        listener = new JwtEmailSyncListener(appUserRepository);
    }

    @Test
    void shouldSetEmailWhenUserHasNoEmail() {
        AppUser user = new AppUser("sub-123");
        assertThat(user.getEmail()).isNull();

        when(appUserRepository.findByKeycloakSub("sub-123")).thenReturn(Optional.of(user));

        listener.onAuthentication(jwtEvent("sub-123", "user@example.com"));

        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void shouldNotOverwriteExistingEmail() {
        AppUser user = new AppUser("sub-123");
        user.setEmail("existing@example.com");

        when(appUserRepository.findByKeycloakSub("sub-123")).thenReturn(Optional.of(user));

        listener.onAuthentication(jwtEvent("sub-123", "new@example.com"));

        assertThat(user.getEmail()).isEqualTo("existing@example.com");
    }

    @Test
    void shouldIgnoreNonJwtAuthentication() {
        var auth = new UsernamePasswordAuthenticationToken("user", "pass");
        var event = new AuthenticationSuccessEvent(auth);

        listener.onAuthentication(event);

        verify(appUserRepository, never()).findByKeycloakSub(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldIgnoreWhenEmailClaimIsMissing() {
        listener.onAuthentication(jwtEvent("sub-123", null));

        verify(appUserRepository, never()).findByKeycloakSub(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldIgnoreWhenUserNotFound() {
        when(appUserRepository.findByKeycloakSub("sub-unknown")).thenReturn(Optional.empty());

        listener.onAuthentication(jwtEvent("sub-unknown", "user@example.com"));

        // No exception thrown
    }

    private AuthenticationSuccessEvent jwtEvent(String sub, String email) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("sub", sub);
        if (email != null) {
            claims.put("email", email);
        }
        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"), claims);
        var auth = new JwtAuthenticationToken(jwt);
        return new AuthenticationSuccessEvent(auth);
    }
}

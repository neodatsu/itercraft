package com.itercraft.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return mock(OpaqueTokenIntrospector.class);
    }
}

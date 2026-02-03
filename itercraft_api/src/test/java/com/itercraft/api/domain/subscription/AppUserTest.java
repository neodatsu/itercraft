package com.itercraft.api.domain.subscription;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    @Test
    void constructor_shouldInitializeAllFields() {
        String keycloakSub = "user-123";

        AppUser user = new AppUser(keycloakSub);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getKeycloakSub()).isEqualTo(keycloakSub);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void constructor_shouldGenerateUniqueIds() {
        AppUser user1 = new AppUser("user-1");
        AppUser user2 = new AppUser("user-2");

        assertThat(user1.getId()).isNotEqualTo(user2.getId());
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        String keycloakSub = "test-sub";
        AppUser user = new AppUser(keycloakSub);

        assertThat(user.getKeycloakSub()).isEqualTo(keycloakSub);
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
    }
}

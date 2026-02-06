package com.itercraft.api.domain.maintenance;

import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.ServiceEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MaintenanceSessionTest {

    @Test
    void start_shouldCreateActiveSession() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);

        MaintenanceSession session = MaintenanceSession.start(user, service);

        assertThat(session.getId()).isNotNull();
        assertThat(session.getUser()).isEqualTo(user);
        assertThat(session.getService()).isEqualTo(service);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.getEndedAt()).isNull();
        assertThat(session.getDurationMinutes()).isNull();
        assertThat(session.getAutoStopped()).isFalse();
        assertThat(session.isActive()).isTrue();
    }

    @Test
    void stop_shouldEndSessionAndCalculateDuration() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        MaintenanceSession session = MaintenanceSession.start(user, service);

        session.stop(false);

        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getDurationMinutes()).isNotNull();
        assertThat(session.getDurationMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(session.getAutoStopped()).isFalse();
        assertThat(session.isActive()).isFalse();
    }

    @Test
    void stop_shouldMarkAsAutoStoppedWhenTrue() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        MaintenanceSession session = MaintenanceSession.start(user, service);

        session.stop(true);

        assertThat(session.getAutoStopped()).isTrue();
    }

    @Test
    void stop_shouldThrowWhenAlreadyStopped() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        MaintenanceSession session = MaintenanceSession.start(user, service);
        session.stop(false);

        assertThatThrownBy(() -> session.stop(false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Session already ended");
    }

    @Test
    void isActive_shouldReturnTrueWhenNotEnded() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        MaintenanceSession session = MaintenanceSession.start(user, service);

        assertThat(session.isActive()).isTrue();
    }

    @Test
    void isActive_shouldReturnFalseWhenEnded() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        MaintenanceSession session = MaintenanceSession.start(user, service);
        session.stop(false);

        assertThat(session.isActive()).isFalse();
    }
}

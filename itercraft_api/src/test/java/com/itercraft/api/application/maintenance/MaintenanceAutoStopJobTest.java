package com.itercraft.api.application.maintenance;

import com.itercraft.api.domain.maintenance.MaintenanceSession;
import com.itercraft.api.domain.maintenance.MaintenanceSessionRepository;
import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.ServiceEntity;
import com.itercraft.api.infrastructure.sse.SseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceAutoStopJobTest {

    @Mock private MaintenanceSessionRepository sessionRepository;
    @Mock private SseService sseService;

    @InjectMocks
    private MaintenanceAutoStopJob autoStopJob;

    @Test
    void autoStopExpiredSessions_shouldStopExpiredSessions() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        when(service.getCode()).thenReturn("tondeuse");
        MaintenanceSession expiredSession = MaintenanceSession.start(user, service);

        when(sessionRepository.findByEndedAtIsNullAndStartedAtBefore(any())).thenReturn(List.of(expiredSession));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        autoStopJob.autoStopExpiredSessions();

        verify(sessionRepository).save(expiredSession);
        verify(sseService).broadcast("maintenance-change");
    }

    @Test
    void autoStopExpiredSessions_shouldNotBroadcastWhenNoExpiredSessions() {
        when(sessionRepository.findByEndedAtIsNullAndStartedAtBefore(any())).thenReturn(List.of());

        autoStopJob.autoStopExpiredSessions();

        verify(sessionRepository, never()).save(any());
        verify(sseService, never()).broadcast(any());
    }

    @Test
    void autoStopExpiredSessions_shouldMarkSessionsAsAutoStopped() {
        AppUser user = new AppUser("user-sub");
        ServiceEntity service = mock(ServiceEntity.class);
        when(service.getCode()).thenReturn("tondeuse");
        MaintenanceSession expiredSession = MaintenanceSession.start(user, service);

        when(sessionRepository.findByEndedAtIsNullAndStartedAtBefore(any())).thenReturn(List.of(expiredSession));
        when(sessionRepository.save(any())).thenAnswer(i -> {
            MaintenanceSession saved = i.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(saved.getAutoStopped()).isTrue();
            return saved;
        });

        autoStopJob.autoStopExpiredSessions();
    }
}

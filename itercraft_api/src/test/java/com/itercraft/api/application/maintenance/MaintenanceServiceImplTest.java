package com.itercraft.api.application.maintenance;

import com.itercraft.api.domain.maintenance.MaintenanceSession;
import com.itercraft.api.domain.maintenance.MaintenanceSessionRepository;
import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.AppUserRepository;
import com.itercraft.api.domain.subscription.ServiceEntity;
import com.itercraft.api.domain.subscription.ServiceRepository;
import com.itercraft.api.domain.subscription.Subscription;
import com.itercraft.api.domain.subscription.SubscriptionRepository;
import com.itercraft.api.infrastructure.sse.SseService;
import com.itercraft.api.infrastructure.web.dto.MaintenanceActivityDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceSessionDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceTotalsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceImplTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MaintenanceSessionRepository maintenanceSessionRepository;
    @Mock private SseService sseService;

    @InjectMocks
    private MaintenanceServiceImpl maintenanceService;

    private static final String SUB = "user-sub-123";
    private static final String SERVICE_CODE = "tondeuse";

    @Test
    void getActivities_shouldReturnAllServicesWithStatus() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Passer la tondeuse");

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(new Subscription(user, service)));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(user, service)).thenReturn(Optional.empty());
        when(maintenanceSessionRepository.sumDurationTodayByService(user, service)).thenReturn(30);

        List<MaintenanceActivityDto> result = maintenanceService.getActivities(SUB);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).serviceCode()).isEqualTo(SERVICE_CODE);
        assertThat(result.get(0).serviceLabel()).isEqualTo("Passer la tondeuse");
        assertThat(result.get(0).isActive()).isFalse();
        assertThat(result.get(0).totalMinutesToday()).isEqualTo(30);
    }

    @Test
    void getActivities_shouldShowActiveSessionWhenExists() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Passer la tondeuse");
        MaintenanceSession activeSession = MaintenanceSession.start(user, service);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(new Subscription(user, service)));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(user, service)).thenReturn(Optional.of(activeSession));
        when(maintenanceSessionRepository.sumDurationTodayByService(user, service)).thenReturn(0);

        List<MaintenanceActivityDto> result = maintenanceService.getActivities(SUB);

        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(0).startedAt()).isEqualTo(activeSession.getStartedAt());
    }

    @Test
    void startActivity_shouldCreateNewSession() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Passer la tondeuse");

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(new Subscription(user, service)));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(user, service)).thenReturn(Optional.empty());
        when(maintenanceSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MaintenanceSessionDto result = maintenanceService.startActivity(SUB, SERVICE_CODE);

        assertThat(result.serviceCode()).isEqualTo(SERVICE_CODE);
        assertThat(result.startedAt()).isNotNull();
        assertThat(result.endedAt()).isNull();
        verify(maintenanceSessionRepository).save(any(MaintenanceSession.class));
        verify(sseService).broadcast("maintenance-change");
    }

    @Test
    void startActivity_shouldThrowWhenAlreadyActive() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        MaintenanceSession activeSession = MaintenanceSession.start(user, service);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(subscriptionRepository.findByUserAndService(user, service)).thenReturn(Optional.of(new Subscription(user, service)));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(user, service)).thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> maintenanceService.startActivity(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session est déjà en cours");
    }

    @Test
    void stopActivity_shouldEndSession() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Passer la tondeuse");
        MaintenanceSession activeSession = MaintenanceSession.start(user, service);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(user, service)).thenReturn(Optional.of(activeSession));
        when(maintenanceSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MaintenanceSessionDto result = maintenanceService.stopActivity(SUB, SERVICE_CODE);

        assertThat(result.endedAt()).isNotNull();
        assertThat(result.autoStopped()).isFalse();
        verify(maintenanceSessionRepository).save(any(MaintenanceSession.class));
        verify(sseService).broadcast("maintenance-change");
    }

    @Test
    void stopActivity_shouldThrowWhenNoActiveSession() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(user, service)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.stopActivity(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucune session active");
    }

    @Test
    void getTotals_shouldReturnAggregatedDurations() {
        AppUser user = new AppUser(SUB);
        UUID userId = user.getId();

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findAll()).thenReturn(List.of());
        when(maintenanceSessionRepository.sumDurationToday(user)).thenReturn(60);
        when(maintenanceSessionRepository.sumDurationThisWeek(userId)).thenReturn(300);
        when(maintenanceSessionRepository.sumDurationThisMonth(userId)).thenReturn(1200);
        when(maintenanceSessionRepository.sumDurationThisYear(userId)).thenReturn(5000);

        MaintenanceTotalsDto result = maintenanceService.getTotals(SUB);

        assertThat(result.todayMinutes()).isEqualTo(60);
        assertThat(result.weekMinutes()).isEqualTo(300);
        assertThat(result.monthMinutes()).isEqualTo(1200);
        assertThat(result.yearMinutes()).isEqualTo(5000);
    }

    @Test
    void getSessionHistory_shouldReturnSessionsForService() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Passer la tondeuse");
        MaintenanceSession session = MaintenanceSession.start(user, service);
        session.stop(false);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.of(service));
        when(maintenanceSessionRepository.findByUserAndServiceOrderByStartedAtDesc(user, service)).thenReturn(List.of(session));

        List<MaintenanceSessionDto> result = maintenanceService.getSessionHistory(SUB, SERVICE_CODE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).serviceCode()).isEqualTo(SERVICE_CODE);
        assertThat(result.get(0).endedAt()).isNotNull();
    }

    @Test
    void getActivities_shouldAutoSubscribeUserToAllServices() {
        AppUser user = new AppUser(SUB);
        ServiceEntity service = mockService();
        when(service.getCode()).thenReturn(SERVICE_CODE);
        when(service.getLabel()).thenReturn("Passer la tondeuse");

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenReturn(user);
        when(serviceRepository.findAll()).thenReturn(List.of(service));
        when(subscriptionRepository.findByUserAndService(any(), any())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(maintenanceSessionRepository.findByUserAndServiceAndEndedAtIsNull(any(), any())).thenReturn(Optional.empty());
        when(maintenanceSessionRepository.sumDurationTodayByService(any(), any())).thenReturn(0);

        maintenanceService.getActivities(SUB);

        verify(appUserRepository).save(any(AppUser.class));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void startActivity_shouldThrowWhenServiceNotFound() {
        AppUser user = new AppUser(SUB);

        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user));
        when(serviceRepository.findAll()).thenReturn(List.of());
        when(serviceRepository.findByCode(SERVICE_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.startActivity(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Service non trouvé");
    }

    @Test
    void stopActivity_shouldThrowWhenUserNotFound() {
        when(appUserRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.stopActivity(SUB, SERVICE_CODE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    private ServiceEntity mockService() {
        return mock(ServiceEntity.class);
    }
}

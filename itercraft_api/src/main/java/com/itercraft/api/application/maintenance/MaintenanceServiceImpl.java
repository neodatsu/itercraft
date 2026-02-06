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
import com.itercraft.api.infrastructure.web.dto.ActivityTotalsDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceActivityDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceSessionDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceTotalsDto;
import jakarta.transaction.Transactional;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {

    private final AppUserRepository appUserRepository;
    private final ServiceRepository serviceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MaintenanceSessionRepository maintenanceSessionRepository;
    private final SseService sseService;
    private static final String SSE_EVENT_TYPE = "maintenance-change";

    public MaintenanceServiceImpl(AppUserRepository appUserRepository,
                                  ServiceRepository serviceRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  MaintenanceSessionRepository maintenanceSessionRepository,
                                  SseService sseService) {
        this.appUserRepository = appUserRepository;
        this.serviceRepository = serviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.maintenanceSessionRepository = maintenanceSessionRepository;
        this.sseService = sseService;
    }

    @Override
    public List<MaintenanceActivityDto> getActivities(String keycloakSub) {
        AppUser user = findOrCreateUserWithAllServices(keycloakSub);
        return serviceRepository.findAll().stream()
                .map(service -> {
                    var activeSession = maintenanceSessionRepository
                            .findByUserAndServiceAndEndedAtIsNull(user, service);
                    int todayMinutes = maintenanceSessionRepository
                            .sumDurationTodayByService(user, service);
                    return new MaintenanceActivityDto(
                            service.getCode(),
                            service.getLabel(),
                            activeSession.isPresent(),
                            activeSession.map(MaintenanceSession::getStartedAt).orElse(null),
                            todayMinutes
                    );
                })
                .toList();
    }

    @Override
    public MaintenanceSessionDto startActivity(String keycloakSub, String serviceCode) {
        AppUser user = findOrCreateUserWithAllServices(keycloakSub);
        ServiceEntity service = findService(serviceCode);

        var existingActive = maintenanceSessionRepository
                .findByUserAndServiceAndEndedAtIsNull(user, service);
        if (existingActive.isPresent()) {
            throw new IllegalStateException("Une session est déjà en cours pour ce service");
        }

        MaintenanceSession session = MaintenanceSession.start(user, service);
        maintenanceSessionRepository.save(session);
        sseService.broadcast(SSE_EVENT_TYPE);

        return toDto(session);
    }

    @Override
    public MaintenanceSessionDto stopActivity(String keycloakSub, String serviceCode) {
        AppUser user = findUser(keycloakSub);
        ServiceEntity service = findService(serviceCode);

        MaintenanceSession session = maintenanceSessionRepository
                .findByUserAndServiceAndEndedAtIsNull(user, service)
                .orElseThrow(() -> new IllegalStateException("Aucune session active pour ce service"));

        session.stop(false);
        maintenanceSessionRepository.save(session);
        sseService.broadcast(SSE_EVENT_TYPE);

        return toDto(session);
    }

    @Override
    public MaintenanceTotalsDto getTotals(String keycloakSub) {
        AppUser user = findOrCreateUserWithAllServices(keycloakSub);
        List<ActivityTotalsDto> byActivity = serviceRepository.findAll().stream()
                .map(service -> new ActivityTotalsDto(
                        service.getCode(),
                        service.getLabel(),
                        maintenanceSessionRepository.sumDurationTodayByService(user, service),
                        maintenanceSessionRepository.sumDurationThisWeekByService(user.getId(), service.getId()),
                        maintenanceSessionRepository.sumDurationThisMonthByService(user.getId(), service.getId()),
                        maintenanceSessionRepository.sumDurationThisYearByService(user.getId(), service.getId())
                ))
                .toList();
        return new MaintenanceTotalsDto(
                maintenanceSessionRepository.sumDurationToday(user),
                maintenanceSessionRepository.sumDurationThisWeek(user.getId()),
                maintenanceSessionRepository.sumDurationThisMonth(user.getId()),
                maintenanceSessionRepository.sumDurationThisYear(user.getId()),
                byActivity
        );
    }

    @Override
    public List<MaintenanceSessionDto> getSessionHistory(String keycloakSub, String serviceCode) {
        AppUser user = findUser(keycloakSub);
        ServiceEntity service = findService(serviceCode);
        return maintenanceSessionRepository.findByUserAndServiceOrderByStartedAtDesc(user, service)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void deleteSession(String keycloakSub, UUID sessionId) {
        AppUser user = findUser(keycloakSub);
        MaintenanceSession session = maintenanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session non trouvée"));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Session non autorisée");
        }

        maintenanceSessionRepository.delete(session);
        sseService.broadcast(SSE_EVENT_TYPE);
    }

    @Override
    public MaintenanceActivityDto createActivity(String keycloakSub, String label) {
        AppUser user = findOrCreateUserWithAllServices(keycloakSub);
        String code = generateCode(label);

        if (serviceRepository.findByCode(code).isPresent()) {
            throw new IllegalStateException("Une activité avec ce nom existe déjà");
        }

        ServiceEntity service = new ServiceEntity(code, label);
        serviceRepository.save(service);
        subscriptionRepository.save(new Subscription(user, service));
        sseService.broadcast(SSE_EVENT_TYPE);

        return new MaintenanceActivityDto(
                service.getCode(),
                service.getLabel(),
                false,
                null,
                0
        );
    }

    private String generateCode(String label) {
        return Normalizer.normalize(label, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("(^_)|(_$)", "");
    }

    private AppUser findOrCreateUserWithAllServices(String keycloakSub) {
        AppUser user = appUserRepository.findByKeycloakSub(keycloakSub)
                .orElseGet(() -> appUserRepository.save(new AppUser(keycloakSub)));

        List<ServiceEntity> allServices = serviceRepository.findAll();
        for (ServiceEntity service : allServices) {
            if (subscriptionRepository.findByUserAndService(user, service).isEmpty()) {
                subscriptionRepository.save(new Subscription(user, service));
            }
        }
        return user;
    }

    private AppUser findUser(String keycloakSub) {
        return appUserRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));
    }

    private ServiceEntity findService(String serviceCode) {
        return serviceRepository.findByCode(serviceCode)
                .orElseThrow(() -> new IllegalStateException("Service non trouvé: " + serviceCode));
    }

    private MaintenanceSessionDto toDto(MaintenanceSession session) {
        return new MaintenanceSessionDto(
                session.getId(),
                session.getService().getCode(),
                session.getService().getLabel(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationMinutes(),
                Boolean.TRUE.equals(session.getAutoStopped())
        );
    }
}

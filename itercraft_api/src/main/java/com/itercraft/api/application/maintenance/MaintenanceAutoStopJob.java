package com.itercraft.api.application.maintenance;

import com.itercraft.api.domain.maintenance.MaintenanceSession;
import com.itercraft.api.domain.maintenance.MaintenanceSessionRepository;
import com.itercraft.api.infrastructure.sse.SseService;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MaintenanceAutoStopJob {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceAutoStopJob.class);
    private static final int MAX_DURATION_HOURS = 4;
    private static final String SSE_EVENT_TYPE = "maintenance-change";

    private final MaintenanceSessionRepository sessionRepository;
    private final SseService sseService;

    public MaintenanceAutoStopJob(MaintenanceSessionRepository sessionRepository, SseService sseService) {
        this.sessionRepository = sessionRepository;
        this.sseService = sseService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoStopExpiredSessions() {
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(MAX_DURATION_HOURS);
        List<MaintenanceSession> expiredSessions =
                sessionRepository.findByEndedAtIsNullAndStartedAtBefore(threshold);

        if (expiredSessions.isEmpty()) {
            return;
        }

        for (MaintenanceSession session : expiredSessions) {
            log.info("Auto-stopping maintenance session {} for service {} (started at {})",
                    session.getId(),
                    session.getService().getCode(),
                    session.getStartedAt());
            session.stop(true);
            sessionRepository.save(session);
        }

        sseService.broadcast(SSE_EVENT_TYPE);
        log.info("Auto-stopped {} expired maintenance session(s)", expiredSessions.size());
    }
}

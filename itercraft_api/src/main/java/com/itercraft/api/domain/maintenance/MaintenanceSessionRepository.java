package com.itercraft.api.domain.maintenance;

import com.itercraft.api.domain.subscription.AppUser;
import com.itercraft.api.domain.subscription.ServiceEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceSessionRepository extends JpaRepository<MaintenanceSession, UUID> {

    Optional<MaintenanceSession> findByUserAndServiceAndEndedAtIsNull(AppUser user, ServiceEntity service);

    List<MaintenanceSession> findByEndedAtIsNullAndStartedAtBefore(OffsetDateTime threshold);

    List<MaintenanceSession> findByUserOrderByStartedAtDesc(AppUser user);

    List<MaintenanceSession> findByUserAndServiceOrderByStartedAtDesc(AppUser user, ServiceEntity service);

    @Query("""
        SELECT COALESCE(SUM(m.durationMinutes), 0)
        FROM MaintenanceSession m
        WHERE m.user = :user
        AND CAST(m.startedAt AS date) = CURRENT_DATE
        """)
    int sumDurationToday(@Param("user") AppUser user);

    @Query("""
        SELECT COALESCE(SUM(m.durationMinutes), 0)
        FROM MaintenanceSession m
        WHERE m.user = :user
        AND m.service = :service
        AND CAST(m.startedAt AS date) = CURRENT_DATE
        """)
    int sumDurationTodayByService(@Param("user") AppUser user, @Param("service") ServiceEntity service);

    @Query(value = """
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM itercraft.maintenance_session
        WHERE user_id = :userId
        AND EXTRACT(ISOYEAR FROM started_at) = EXTRACT(ISOYEAR FROM CURRENT_DATE)
        AND EXTRACT(WEEK FROM started_at) = EXTRACT(WEEK FROM CURRENT_DATE)
        """, nativeQuery = true)
    int sumDurationThisWeek(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM itercraft.maintenance_session
        WHERE user_id = :userId
        AND EXTRACT(YEAR FROM started_at) = EXTRACT(YEAR FROM CURRENT_DATE)
        AND EXTRACT(MONTH FROM started_at) = EXTRACT(MONTH FROM CURRENT_DATE)
        """, nativeQuery = true)
    int sumDurationThisMonth(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM itercraft.maintenance_session
        WHERE user_id = :userId
        AND EXTRACT(YEAR FROM started_at) = EXTRACT(YEAR FROM CURRENT_DATE)
        """, nativeQuery = true)
    int sumDurationThisYear(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM itercraft.maintenance_session
        WHERE user_id = :userId
        AND service_id = :serviceId
        AND EXTRACT(ISOYEAR FROM started_at) = EXTRACT(ISOYEAR FROM CURRENT_DATE)
        AND EXTRACT(WEEK FROM started_at) = EXTRACT(WEEK FROM CURRENT_DATE)
        """, nativeQuery = true)
    int sumDurationThisWeekByService(@Param("userId") UUID userId, @Param("serviceId") UUID serviceId);

    @Query(value = """
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM itercraft.maintenance_session
        WHERE user_id = :userId
        AND service_id = :serviceId
        AND EXTRACT(YEAR FROM started_at) = EXTRACT(YEAR FROM CURRENT_DATE)
        AND EXTRACT(MONTH FROM started_at) = EXTRACT(MONTH FROM CURRENT_DATE)
        """, nativeQuery = true)
    int sumDurationThisMonthByService(@Param("userId") UUID userId, @Param("serviceId") UUID serviceId);

    @Query(value = """
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM itercraft.maintenance_session
        WHERE user_id = :userId
        AND service_id = :serviceId
        AND EXTRACT(YEAR FROM started_at) = EXTRACT(YEAR FROM CURRENT_DATE)
        """, nativeQuery = true)
    int sumDurationThisYearByService(@Param("userId") UUID userId, @Param("serviceId") UUID serviceId);
}

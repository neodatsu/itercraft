package com.itercraft.api.domain.sensor;

import com.itercraft.api.domain.subscription.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SensorDataRepository extends JpaRepository<SensorData, UUID> {

    @Query("""
        SELECT sd FROM SensorData sd
        WHERE sd.device.user = :user
        AND sd.measuredAt BETWEEN :from AND :to
        ORDER BY sd.measuredAt ASC
        """)
    List<SensorData> findByUserAndDateRange(
        @Param("user") AppUser user,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );
}

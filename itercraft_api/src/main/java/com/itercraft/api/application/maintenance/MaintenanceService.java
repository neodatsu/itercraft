package com.itercraft.api.application.maintenance;

import com.itercraft.api.infrastructure.web.dto.MaintenanceActivityDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceSessionDto;
import com.itercraft.api.infrastructure.web.dto.MaintenanceTotalsDto;
import java.util.List;
import java.util.UUID;

public interface MaintenanceService {
    List<MaintenanceActivityDto> getActivities(String keycloakSub);
    MaintenanceSessionDto startActivity(String keycloakSub, String serviceCode);
    MaintenanceSessionDto stopActivity(String keycloakSub, String serviceCode);
    MaintenanceTotalsDto getTotals(String keycloakSub);
    List<MaintenanceSessionDto> getSessionHistory(String keycloakSub, String serviceCode);
    void deleteSession(String keycloakSub, UUID sessionId);
    MaintenanceActivityDto createActivity(String keycloakSub, String label);
}

package daniil.pavelev.incident.api;

import daniil.pavelev.incident.api.dto.IncidentStatisticsResponse;
import daniil.pavelev.incident.service.IncidentStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incident-statistics")
public class IncidentStatisticsController {

    private final IncidentStatisticsService statisticsService;

    public IncidentStatisticsController(IncidentStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public IncidentStatisticsResponse statistics() {
        return statisticsService.statistics();
    }
}

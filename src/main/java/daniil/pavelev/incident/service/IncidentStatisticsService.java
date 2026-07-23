package daniil.pavelev.incident.service;

import daniil.pavelev.incident.api.dto.IncidentStatisticsResponse;
import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.persistence.IncidentAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentStatisticsService {

    private final IncidentAnalysisRepository repository;

    public IncidentStatisticsService(IncidentAnalysisRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public IncidentStatisticsResponse statistics() {
        long total = repository.count();
        long completed = repository.countByStatus(AnalysisStatus.COMPLETED);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : repository.countByCategory()) {
            byCategory.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : repository.countBySeverity()) {
            bySeverity.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        List<IncidentStatisticsResponse.NamedCount> services = new ArrayList<>();
        for (Object[] row : repository.mostFrequentServices(10)) {
            services.add(new IncidentStatisticsResponse.NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        }

        List<IncidentStatisticsResponse.NamedCount> providers = new ArrayList<>();
        for (Object[] row : repository.mostFrequentProviders(10)) {
            providers.add(new IncidentStatisticsResponse.NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        }

        List<IncidentStatisticsResponse.TimeBucket> overTime = new ArrayList<>();
        for (Object[] row : repository.analysesOverTimeByDay()) {
            LocalDate day = row[0] instanceof LocalDate localDate
                    ? localDate
                    : LocalDate.parse(String.valueOf(row[0]));
            overTime.add(new IncidentStatisticsResponse.TimeBucket(day, ((Number) row[1]).longValue()));
        }

        return new IncidentStatisticsResponse(
                total,
                completed,
                byCategory,
                bySeverity,
                services,
                providers,
                overTime
        );
    }
}

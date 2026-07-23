package daniil.pavelev.incident.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record IncidentStatisticsResponse(
        long totalAnalyses,
        long completedAnalyses,
        Map<String, Long> countByCategory,
        Map<String, Long> countBySeverity,
        List<NamedCount> mostFrequentServices,
        List<NamedCount> mostFrequentProviders,
        List<TimeBucket> analysesOverTime
) {
    public record NamedCount(String name, long count) {
    }

    public record TimeBucket(LocalDate day, long count) {
    }
}

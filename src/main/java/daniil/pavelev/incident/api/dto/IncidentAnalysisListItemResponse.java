package daniil.pavelev.incident.api.dto;

import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record IncidentAnalysisListItemResponse(
        UUID id,
        IncidentCategory category,
        String summary,
        IncidentSeverity severity,
        AnalysisStatus status,
        Instant createdAt,
        Instant completedAt,
        Set<String> mentionedServices,
        Set<String> providerNames,
        String errorCode
) {
}

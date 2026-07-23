package daniil.pavelev.incident.memory;

import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;

import java.util.List;
import java.util.UUID;

public record CompactMemoryEntry(
        UUID analysisId,
        IncidentCategory category,
        String summary,
        IncidentSeverity severity,
        List<String> matchingServices,
        List<String> matchingProviders,
        List<String> usefulDiagnosticSteps,
        double matchScore
) {
}

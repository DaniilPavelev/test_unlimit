package daniil.pavelev.api.dto;

import java.util.List;

public record IncidentAnalysisMetadataResponse(
        List<String> mentionedServices,
        List<String> mentionedProviders,
        List<String> selectedHistoricalAnalysisIds,
        int llmAttempts
) {
}

package daniil.pavelev.domain;

import java.util.List;

public record AnalysisMetadata(
        List<String> mentionedServices,
        List<String> mentionedProviders,
        List<String> selectedHistoricalAnalysisIds,
        int llmAttempts
) {
}

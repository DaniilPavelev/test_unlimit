package daniil.pavelev.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentAnalysisListItemResponse(
        UUID id,
        Instant createdAt,
        String category,
        String summary,
        String severity,
        List<String> mentionedServices,
        List<String> mentionedProviders
) {
}

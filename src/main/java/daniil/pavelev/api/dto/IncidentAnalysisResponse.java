package daniil.pavelev.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentAnalysisResponse(
        UUID id,
        Instant createdAt,
        String category,
        String summary,
        String severity,
        List<HypothesisResponse> hypotheses,
        IncidentAnalysisMetadataResponse metadata
) {
}

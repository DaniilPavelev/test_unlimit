package daniil.pavelev.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentAnalysis(
        UUID id,
        Instant createdAt,
        String originalDescription,
        String normalizedDescription,
        String category,
        String summary,
        Severity severity,
        List<Hypothesis> hypotheses,
        AnalysisMetadata metadata
) {
}

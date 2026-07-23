package daniil.pavelev.domain;

import java.util.List;

public record LlmAnalysisPayload(
        String category,
        String summary,
        String severity,
        List<HypothesisDraft> hypotheses
) {
    public record HypothesisDraft(
            String title,
            String reasoning,
            List<String> nextSteps
    ) {
    }
}

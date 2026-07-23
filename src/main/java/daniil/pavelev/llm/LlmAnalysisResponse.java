package daniil.pavelev.llm;

import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;

import java.util.List;

public record LlmAnalysisResponse(
        IncidentCategory category,
        IncidentSeverity severity,
        String summary,
        List<String> hypotheses,
        List<String> nextSteps,
        String model,
        int attempts,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String rawPromptSnapshot
) {
}

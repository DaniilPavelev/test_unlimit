package daniil.pavelev.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniil.pavelev.config.LlmProperties;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import daniil.pavelev.incident.memory.CompactAggregateMemory;
import daniil.pavelev.incident.memory.CompactMemoryEntry;
import daniil.pavelev.incident.signal.ExtractedSignals;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Deterministic stub LLM used by default and in tests. Never calls an external provider.
 */
@Component
@ConditionalOnProperty(name = "llm.mode", havingValue = "stub", matchIfMissing = true)
public class StubLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public StubLlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmAnalysisResponse analyze(LlmAnalysisRequest request) {
        String promptSnapshot = buildPromptSnapshot(request);
        IncidentCategory category = inferCategory(request.signals(), request.normalizedDescription());
        IncidentSeverity severity = inferSeverity(request.signals(), request.normalizedDescription());
        String summary = "Analyzed incident involving "
                + summarizeSet(request.signals().mentionedServices())
                + " with providers "
                + summarizeSet(request.signals().providerNames())
                + ".";

        List<String> hypotheses = new ArrayList<>();
        if (!request.historicalMemory().isEmpty()) {
            hypotheses.add("Similar to prior incident "
                    + request.historicalMemory().getFirst().analysisId()
                    + " with score "
                    + request.historicalMemory().getFirst().matchScore());
        } else {
            hypotheses.add("No strongly matching historical incident found.");
        }
        hypotheses.add("Likely " + category.name().toLowerCase(Locale.ROOT) + " regression.");

        List<String> nextSteps = new ArrayList<>();
        nextSteps.add("Verify health of mentioned services.");
        if (!request.signals().httpStatusCodes().isEmpty()) {
            nextSteps.add("Inspect upstream responses returning " + request.signals().httpStatusCodes());
        }
        nextSteps.add("Compare with selected historical memory only (not full history).");

        int promptTokens = promptSnapshot.length() / 4;
        int completionTokens = (summary.length() + hypotheses.toString().length()) / 4;

        return new LlmAnalysisResponse(
                category,
                severity,
                summary,
                hypotheses,
                nextSteps,
                properties.getModel(),
                1,
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                promptSnapshot
        );
    }

    @Override
    public LlmCompactionResponse compactTextualPatterns(LlmCompactionRequest request) {
        String recurring = request.summaries().stream().limit(5).collect(Collectors.joining(" | "));
        String diagnostics = request.diagnosticSteps().stream().limit(5).collect(Collectors.joining(" | "));
        return new LlmCompactionResponse(
                recurring.isBlank() ? "No recurring textual patterns." : recurring,
                diagnostics.isBlank() ? "No diagnostic patterns." : diagnostics,
                properties.getModel()
        );
    }

    private String buildPromptSnapshot(LlmAnalysisRequest request) {
        try {
            return objectMapper.writeValueAsString(new PromptView(
                    request.normalizedDescription(),
                    request.signals(),
                    request.staticKnowledge(),
                    request.historicalMemory(),
                    request.aggregateMemory()
            ));
        } catch (Exception e) {
            return "normalized=" + request.normalizedDescription()
                    + "; historyIds=" + request.historicalMemory().stream().map(CompactMemoryEntry::analysisId).toList()
                    + "; aggregateIds=" + request.aggregateMemory().stream().map(CompactAggregateMemory::aggregateId).toList();
        }
    }

    private IncidentCategory inferCategory(ExtractedSignals signals, String description) {
        String lower = description == null ? "" : description.toLowerCase(Locale.ROOT);
        if (lower.contains("timeout") || lower.contains("latency") || lower.contains("slow")) {
            return IncidentCategory.PERFORMANCE;
        }
        if (lower.contains("unauthorized") || lower.contains("auth") || lower.contains("breach")) {
            return IncidentCategory.SECURITY;
        }
        if (!signals.httpStatusCodes().isEmpty()) {
            return IncidentCategory.DEPENDENCY;
        }
        if (lower.contains("config") || lower.contains("misconfig")) {
            return IncidentCategory.CONFIGURATION;
        }
        if (lower.contains("down") || lower.contains("unavailable") || lower.contains("outage")) {
            return IncidentCategory.AVAILABILITY;
        }
        return IncidentCategory.UNKNOWN;
    }

    private IncidentSeverity inferSeverity(ExtractedSignals signals, String description) {
        String lower = description == null ? "" : description.toLowerCase(Locale.ROOT);
        if (lower.contains("critical") || lower.contains("outage") || signals.httpStatusCodes().contains(500)) {
            return IncidentSeverity.CRITICAL;
        }
        if (lower.contains("degraded") || signals.httpStatusCodes().contains(503)) {
            return IncidentSeverity.HIGH;
        }
        if (lower.contains("intermittent")) {
            return IncidentSeverity.MEDIUM;
        }
        return IncidentSeverity.LOW;
    }

    private String summarizeSet(java.util.Set<String> values) {
        if (values.isEmpty()) {
            return "unknown services";
        }
        return String.join(", ", values);
    }

    private record PromptView(
            String currentIncident,
            ExtractedSignals signals,
            List<String> staticKnowledge,
            List<CompactMemoryEntry> historicalMemory,
            List<CompactAggregateMemory> aggregateMemory
    ) {
    }
}

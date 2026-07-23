package daniil.pavelev.incident.api.dto;

import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record IncidentAnalysisDetailResponse(
        UUID id,
        String originalDescription,
        String normalizedDescription,
        IncidentCategory category,
        String summary,
        IncidentSeverity severity,
        AnalysisStatus status,
        Instant createdAt,
        Instant completedAt,
        String model,
        Integer llmAttempts,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Long processingDurationMs,
        String errorCode,
        List<String> hypotheses,
        List<String> nextSteps,
        Set<String> mentionedServices,
        Set<String> providerNames,
        Set<Integer> httpStatusCodes,
        Set<String> keywords,
        Set<String> affectedFunctionality,
        List<UUID> memoryAnalysisIdsUsed,
        List<UUID> memoryAggregateIdsUsed
) {
}

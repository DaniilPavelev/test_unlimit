package daniil.pavelev.llm;

import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import daniil.pavelev.incident.memory.CompactAggregateMemory;
import daniil.pavelev.incident.memory.CompactMemoryEntry;
import daniil.pavelev.incident.signal.ExtractedSignals;

import java.util.List;

public record LlmAnalysisRequest(
        String normalizedDescription,
        ExtractedSignals signals,
        List<CompactMemoryEntry> historicalMemory,
        List<CompactAggregateMemory> aggregateMemory,
        List<String> staticKnowledge
) {
}

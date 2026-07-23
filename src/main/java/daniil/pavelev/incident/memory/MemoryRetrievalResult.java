package daniil.pavelev.incident.memory;

import java.util.List;
import java.util.UUID;

public record MemoryRetrievalResult(
        List<CompactMemoryEntry> selectedIncidents,
        List<CompactAggregateMemory> selectedAggregates,
        List<UUID> selectedAnalysisIds,
        List<UUID> selectedAggregateIds,
        List<Double> matchScores,
        int estimatedContextCharacters
) {
    public static MemoryRetrievalResult empty() {
        return new MemoryRetrievalResult(List.of(), List.of(), List.of(), List.of(), List.of(), 0);
    }
}

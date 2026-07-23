package daniil.pavelev.incident.memory;

import java.util.List;
import java.util.UUID;

public record CompactAggregateMemory(
        UUID aggregateId,
        String recurringPatterns,
        String effectiveDiagnosticPatterns,
        List<String> frequentServices,
        List<String> frequentProviders,
        double matchScore
) {
}

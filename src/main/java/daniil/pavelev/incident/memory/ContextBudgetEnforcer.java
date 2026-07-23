package daniil.pavelev.incident.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enforces a configurable character budget without producing invalid JSON fragments.
 * Reduction order: drop lowest-ranked incidents, then aggregates, then shorten summaries/steps.
 */
@Component
public class ContextBudgetEnforcer {

    private final ObjectMapper objectMapper;

    public ContextBudgetEnforcer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BudgetedMemory enforce(
            List<CompactMemoryEntry> incidents,
            List<CompactAggregateMemory> aggregates,
            int maxCharacters
    ) {
        List<CompactMemoryEntry> workingIncidents = new ArrayList<>(incidents);
        List<CompactAggregateMemory> workingAggregates = new ArrayList<>(aggregates);
        workingIncidents.sort(Comparator.comparingDouble(CompactMemoryEntry::matchScore).reversed());
        workingAggregates.sort(Comparator.comparingDouble(CompactAggregateMemory::matchScore).reversed());

        while (estimate(workingIncidents, workingAggregates) > maxCharacters) {
            if (workingIncidents.size() > 1) {
                workingIncidents.removeLast();
                continue;
            }
            if (workingAggregates.size() > 1) {
                workingAggregates.removeLast();
                continue;
            }
            if (!workingIncidents.isEmpty() && shortenIncidentInPlace(workingIncidents, 0)) {
                continue;
            }
            if (!workingAggregates.isEmpty() && shortenAggregateInPlace(workingAggregates, 0)) {
                continue;
            }
            workingIncidents.clear();
            workingAggregates.clear();
            break;
        }

        return new BudgetedMemory(
                List.copyOf(workingIncidents),
                List.copyOf(workingAggregates),
                estimate(workingIncidents, workingAggregates)
        );
    }

    public int estimate(List<CompactMemoryEntry> incidents, List<CompactAggregateMemory> aggregates) {
        try {
            return objectMapper.writeValueAsString(new MemoryPayload(incidents, aggregates)).length();
        } catch (JsonProcessingException e) {
            int fallback = 0;
            for (CompactMemoryEntry entry : incidents) {
                fallback += length(entry.summary())
                        + entry.matchingServices().stream().mapToInt(this::length).sum()
                        + entry.matchingProviders().stream().mapToInt(this::length).sum()
                        + entry.usefulDiagnosticSteps().stream().mapToInt(this::length).sum()
                        + 64;
            }
            for (CompactAggregateMemory aggregate : aggregates) {
                fallback += length(aggregate.recurringPatterns())
                        + length(aggregate.effectiveDiagnosticPatterns())
                        + 64;
            }
            return fallback;
        }
    }

    private boolean shortenIncidentInPlace(List<CompactMemoryEntry> incidents, int index) {
        CompactMemoryEntry entry = incidents.get(index);
        List<String> steps = new ArrayList<>(entry.usefulDiagnosticSteps());
        if (steps.size() > 1) {
            steps.removeLast();
            incidents.set(index, withIncident(entry, shortenText(entry.summary()), steps));
            return true;
        }
        String summary = nullToEmpty(entry.summary());
        if (summary.length() > 40) {
            incidents.set(index, withIncident(entry, truncate(summary, Math.max(40, summary.length() / 2)), steps));
            return true;
        }
        if (!steps.isEmpty()) {
            incidents.set(index, withIncident(entry, summary, List.of()));
            return true;
        }
        return false;
    }

    private boolean shortenAggregateInPlace(List<CompactAggregateMemory> aggregates, int index) {
        CompactAggregateMemory aggregate = aggregates.get(index);
        String patterns = nullToEmpty(aggregate.recurringPatterns());
        String diagnostics = nullToEmpty(aggregate.effectiveDiagnosticPatterns());
        if (patterns.length() > 80) {
            aggregates.set(index, withAggregate(aggregate, truncate(patterns, Math.max(40, patterns.length() / 2)), diagnostics));
            return true;
        }
        if (diagnostics.length() > 80) {
            aggregates.set(index, withAggregate(aggregate, patterns, truncate(diagnostics, Math.max(40, diagnostics.length() / 2))));
            return true;
        }
        return false;
    }

    private CompactMemoryEntry withIncident(CompactMemoryEntry entry, String summary, List<String> steps) {
        return new CompactMemoryEntry(
                entry.analysisId(),
                entry.category(),
                summary,
                entry.severity(),
                entry.matchingServices(),
                entry.matchingProviders(),
                List.copyOf(steps),
                entry.matchScore()
        );
    }

    private CompactAggregateMemory withAggregate(CompactAggregateMemory aggregate, String patterns, String diagnostics) {
        return new CompactAggregateMemory(
                aggregate.aggregateId(),
                patterns,
                diagnostics,
                aggregate.frequentServices(),
                aggregate.frequentProviders(),
                aggregate.matchScore()
        );
    }

    private String shortenText(String summary) {
        String value = nullToEmpty(summary);
        return truncate(value, Math.max(40, value.length() * 3 / 4));
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max).trim() + "…";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private record MemoryPayload(List<CompactMemoryEntry> incidents, List<CompactAggregateMemory> aggregates) {
    }

    public record BudgetedMemory(
            List<CompactMemoryEntry> incidents,
            List<CompactAggregateMemory> aggregates,
            int estimatedCharacters
    ) {
    }
}

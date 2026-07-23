package daniil.pavelev.incident.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import daniil.pavelev.config.MemoryRetrievalProperties;
import daniil.pavelev.incident.domain.AnalysisStatus;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.domain.IncidentSeverity;
import daniil.pavelev.incident.domain.OrderedTextItem;
import daniil.pavelev.incident.signal.ExtractedSignals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RelevanceScorerTest {

    private RelevanceScorer scorer;

    @BeforeEach
    void setUp() {
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setRecentBonus(1.0);
        properties.setRecentDays(14);
        scorer = new RelevanceScorer(properties);
    }

    @Test
    void scoresExplainableWeights() {
        IncidentAnalysisEntity candidate = baseEntity();
        candidate.setCategory(IncidentCategory.DEPENDENCY);
        candidate.setMentionedServices(Set.of("payment-service"));
        candidate.setProviderNames(Set.of("Stripe"));
        candidate.setHttpStatusCodes(Set.of(502));
        candidate.setKeywords(Set.of("timeout", "payment"));
        candidate.setCreatedAt(Instant.now());

        ExtractedSignals query = new ExtractedSignals(
                Set.of("payment-service"),
                Set.of("Stripe"),
                Set.of(502),
                Set.of("timeout", "payment"),
                Set.of()
        );

        RelevanceScorer.ScoredCandidate scored = scorer.score(
                candidate,
                query,
                IncidentCategory.DEPENDENCY,
                Instant.now()
        );

        assertThat(scored.score()).isEqualTo(17.0);
        assertThat(scored.matchingServices()).containsExactly("payment-service");
        assertThat(scored.matchingProviders()).containsExactly("stripe");
    }

    private static IncidentAnalysisEntity baseEntity() {
        IncidentAnalysisEntity entity = new IncidentAnalysisEntity(
                UUID.randomUUID(),
                "original",
                "normalized",
                Instant.now().minusSeconds(60)
        );
        entity.setStatus(AnalysisStatus.COMPLETED);
        entity.setSummary("summary");
        entity.setSeverity(IncidentSeverity.MEDIUM);
        entity.setNextSteps(List.of(new OrderedTextItem(0, "check dashboards")));
        entity.setHypotheses(new ArrayList<>());
        entity.setMentionedServices(new LinkedHashSet<>());
        entity.setProviderNames(new LinkedHashSet<>());
        entity.setHttpStatusCodes(new LinkedHashSet<>());
        entity.setKeywords(new LinkedHashSet<>());
        entity.setAffectedFunctionality(new LinkedHashSet<>());
        return entity;
    }
}

class ContextBudgetEnforcerTest {

    private ContextBudgetEnforcer enforcer;

    @BeforeEach
    void setUp() {
        enforcer = new ContextBudgetEnforcer(new ObjectMapper());
    }

    @Test
    void enforcesContextBudgetByReducingIncidents() {
        List<CompactMemoryEntry> incidents = IntStream.range(0, 8)
                .mapToObj(i -> entry(i, 100 - i, "summary-" + i + "-".repeat(40)))
                .toList();

        ContextBudgetEnforcer.BudgetedMemory budgeted = enforcer.enforce(incidents, List.of(), 500);

        assertThat(budgeted.incidents()).hasSizeLessThanOrEqualTo(5);
        assertThat(budgeted.estimatedCharacters()).isLessThanOrEqualTo(500);
    }

    @Test
    void shortensWithoutCreatingInvalidFragments() {
        CompactMemoryEntry large = new CompactMemoryEntry(
                UUID.randomUUID(),
                IncidentCategory.AVAILABILITY,
                "x".repeat(2000),
                IncidentSeverity.HIGH,
                List.of("svc-a"),
                List.of("AWS"),
                List.of("step1", "step2", "step3", "step4", "step5"),
                20.0
        );

        ContextBudgetEnforcer.BudgetedMemory budgeted = enforcer.enforce(List.of(large), List.of(), 300);

        assertThat(budgeted.estimatedCharacters()).isLessThanOrEqualTo(300);
        if (!budgeted.incidents().isEmpty()) {
            CompactMemoryEntry kept = budgeted.incidents().getFirst();
            assertThat(kept.usefulDiagnosticSteps()).isNotNull();
            assertThat(kept.summary()).isNotNull();
        }
    }

    private CompactMemoryEntry entry(int index, double score, String summary) {
        return new CompactMemoryEntry(
                UUID.randomUUID(),
                IncidentCategory.UNKNOWN,
                summary,
                IncidentSeverity.LOW,
                List.of("svc-" + index),
                List.of(),
                List.of("check logs"),
                score
        );
    }
}

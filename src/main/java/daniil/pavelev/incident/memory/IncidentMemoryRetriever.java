package daniil.pavelev.incident.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import daniil.pavelev.config.MemoryRetrievalProperties;
import daniil.pavelev.incident.domain.AggregateMemoryEntity;
import daniil.pavelev.incident.domain.IncidentAnalysisEntity;
import daniil.pavelev.incident.domain.IncidentCategory;
import daniil.pavelev.incident.persistence.AggregateMemoryRepository;
import daniil.pavelev.incident.persistence.IncidentAnalysisRepository;
import daniil.pavelev.incident.signal.ExtractedSignals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class IncidentMemoryRetriever implements MemoryRetrievalStrategy {

    private static final Logger log = LoggerFactory.getLogger(IncidentMemoryRetriever.class);

    private final IncidentAnalysisRepository analysisRepository;
    private final AggregateMemoryRepository aggregateMemoryRepository;
    private final RelevanceScorer relevanceScorer;
    private final ContextBudgetEnforcer contextBudgetEnforcer;
    private final MemoryRetrievalProperties properties;
    private final ObjectMapper objectMapper;

    public IncidentMemoryRetriever(
            IncidentAnalysisRepository analysisRepository,
            AggregateMemoryRepository aggregateMemoryRepository,
            RelevanceScorer relevanceScorer,
            ContextBudgetEnforcer contextBudgetEnforcer,
            MemoryRetrievalProperties properties,
            ObjectMapper objectMapper
    ) {
        this.analysisRepository = analysisRepository;
        this.aggregateMemoryRepository = aggregateMemoryRepository;
        this.relevanceScorer = relevanceScorer;
        this.contextBudgetEnforcer = contextBudgetEnforcer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryRetrievalResult retrieve(ExtractedSignals signals, IncidentCategory inferredCategory) {
        Instant now = Instant.now();
        Instant from = now.minus(properties.getLookbackDays(), ChronoUnit.DAYS);

        List<IncidentAnalysisEntity> candidates = analysisRepository.findCompletedBetween(from, now);
        List<RelevanceScorer.ScoredCandidate> scored = candidates.stream()
                .map(candidate -> relevanceScorer.score(candidate, signals, inferredCategory, now))
                .filter(c -> c.score() > 0)
                .sorted(Comparator.comparingDouble(RelevanceScorer.ScoredCandidate::score).reversed())
                .limit(properties.getMaxIncidents())
                .toList();

        List<CompactMemoryEntry> incidentEntries = scored.stream()
                .map(this::toCompactEntry)
                .toList();

        List<CompactAggregateMemory> aggregates = selectAggregates(signals, inferredCategory);

        ContextBudgetEnforcer.BudgetedMemory budgeted = contextBudgetEnforcer.enforce(
                incidentEntries,
                aggregates,
                properties.getMaxContextCharacters()
        );

        List<UUID> analysisIds = budgeted.incidents().stream().map(CompactMemoryEntry::analysisId).toList();
        List<UUID> aggregateIds = budgeted.aggregates().stream().map(CompactAggregateMemory::aggregateId).toList();
        List<Double> scores = new ArrayList<>();
        budgeted.incidents().forEach(e -> scores.add(e.matchScore()));
        budgeted.aggregates().forEach(e -> scores.add(e.matchScore()));

        return new MemoryRetrievalResult(
                budgeted.incidents(),
                budgeted.aggregates(),
                analysisIds,
                aggregateIds,
                List.copyOf(scores),
                budgeted.estimatedCharacters()
        );
    }

    public MemoryRetrievalResult retrieveSafely(ExtractedSignals signals, IncidentCategory inferredCategory) {
        try {
            return retrieve(signals, inferredCategory);
        } catch (Exception ex) {
            log.warn("Memory retrieval failed; continuing without historical memory", ex);
            return MemoryRetrievalResult.empty();
        }
    }

    private CompactMemoryEntry toCompactEntry(RelevanceScorer.ScoredCandidate scored) {
        IncidentAnalysisEntity entity = scored.entity();
        String summary = entity.getSummary() == null ? "" : entity.getSummary();
        List<String> steps = scored.diagnosticSteps().stream().limit(5).toList();
        return new CompactMemoryEntry(
                entity.getId(),
                entity.getCategory(),
                summary,
                entity.getSeverity(),
                scored.matchingServices(),
                scored.matchingProviders(),
                steps,
                scored.score()
        );
    }

    private List<CompactAggregateMemory> selectAggregates(ExtractedSignals signals, IncidentCategory inferredCategory) {
        List<AggregateMemoryEntity> all = aggregateMemoryRepository.findAllNewestFirst();
        List<CompactAggregateMemory> scored = new ArrayList<>();
        for (AggregateMemoryEntity aggregate : all) {
            double score = scoreAggregate(aggregate, signals, inferredCategory);
            if (score <= 0) {
                continue;
            }
            scored.add(new CompactAggregateMemory(
                    aggregate.getId(),
                    aggregate.getRecurringPatterns(),
                    aggregate.getEffectiveDiagnosticPatterns(),
                    readStringList(aggregate.getFrequentServicesJson()),
                    readStringList(aggregate.getFrequentProvidersJson()),
                    score
            ));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(CompactAggregateMemory::matchScore).reversed())
                .limit(properties.getMaxAggregateMemories())
                .toList();
    }

    private double scoreAggregate(
            AggregateMemoryEntity aggregate,
            ExtractedSignals signals,
            IncidentCategory inferredCategory
    ) {
        double score = 0;
        List<String> services = readStringList(aggregate.getFrequentServicesJson());
        List<String> providers = readStringList(aggregate.getFrequentProvidersJson());
        Map<String, Long> categories = readCountMap(aggregate.getCategoryCountsJson());

        for (String service : services) {
            if (signals.mentionedServicesLower().contains(service.toLowerCase(Locale.ROOT))) {
                score += RelevanceScorer.SERVICE_WEIGHT;
            }
        }
        for (String provider : providers) {
            if (signals.providerNamesLower().contains(provider.toLowerCase(Locale.ROOT))) {
                score += RelevanceScorer.PROVIDER_WEIGHT;
            }
        }
        if (inferredCategory != null && categories.containsKey(inferredCategory.name())) {
            score += RelevanceScorer.CATEGORY_WEIGHT;
        }
        return score;
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Long> readCountMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }
}
